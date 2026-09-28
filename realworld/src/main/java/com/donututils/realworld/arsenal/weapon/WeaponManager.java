package com.donututils.realworld.arsenal.weapon;

import com.donututils.realworld.arsenal.config.ArsenalConfig;
import com.donututils.realworld.arsenal.config.WeaponDefinition;
import com.donututils.realworld.arsenal.model.PlayerWeaponState;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Core ballistics: raycast hit detection, head/body/leg damage multipliers, and spread-based
 * "recoil" that grows with sustained fire and decays once you let off the trigger. Ammo and weapon
 * identity live on the ItemStack's own PersistentDataContainer (see {@link WeaponKeys}), so items
 * keep working correctly across drops/pickups/inventory moves without a separate lookup table.
 */
public final class WeaponManager {

    private final Plugin plugin;
    private final WeaponKeys keys;
    private final WeaponItemFactory itemFactory;
    private final Supplier<ArsenalConfig> configSupplier;
    private final Random random = new Random();

    private final Map<UUID, PlayerWeaponState> states = new ConcurrentHashMap<>();

    public WeaponManager(Plugin plugin, WeaponKeys keys, WeaponItemFactory itemFactory, Supplier<ArsenalConfig> configSupplier) {
        this.plugin = plugin;
        this.keys = keys;
        this.itemFactory = itemFactory;
        this.configSupplier = configSupplier;
    }

    public WeaponDefinition identify(ItemStack item) {
        if (item == null || item.getItemMeta() == null) {
            return null;
        }
        String id = item.getItemMeta().getPersistentDataContainer().get(keys.weaponId, PersistentDataType.STRING);
        return id == null ? null : configSupplier.get().weapon(id);
    }

    public int getAmmo(ItemStack weaponItem) {
        if (weaponItem == null || weaponItem.getItemMeta() == null) {
            return 0;
        }
        Integer ammo = weaponItem.getItemMeta().getPersistentDataContainer().get(keys.ammoCount, PersistentDataType.INTEGER);
        return ammo == null ? 0 : ammo;
    }

    private PlayerWeaponState stateFor(UUID playerId) {
        return states.computeIfAbsent(playerId, id -> new PlayerWeaponState());
    }

    public void fire(Player shooter, ItemStack weaponItem, WeaponDefinition definition) {
        PlayerWeaponState state = stateFor(shooter.getUniqueId());
        long now = System.currentTimeMillis();

        if (state.isReloading()) {
            return;
        }
        if (now - state.lastShotAtMillis() < definition.fireCooldownMs()) {
            return;
        }
        int ammo = getAmmo(weaponItem);
        if (ammo <= 0) {
            shooter.sendMessage(color("&cOut of ammo - reload (swap-hands key)."));
            return;
        }

        double elapsedSeconds = (now - state.lastShotAtMillis()) / 1000.0;
        double decayed = state.currentSpreadDegrees() - definition.decayPerSecondDegrees() * elapsedSeconds;
        double spreadBeforeShot = Math.max(definition.baseSpreadDegrees(), decayed);

        Location eyeLocation = shooter.getEyeLocation();
        Vector direction = applySpread(eyeLocation.getDirection(), spreadBeforeShot);

        World world = shooter.getWorld();
        if (definition.splashRadius() > 0) {
            fireExplosive(shooter, world, eyeLocation, direction, definition);
        } else {
            RayTraceResult result = world.rayTraceEntities(eyeLocation, direction, definition.maxRange(), 0.3,
                    candidate -> candidate instanceof LivingEntity && !candidate.equals(shooter));

            if (result != null && result.getHitEntity() instanceof LivingEntity target) {
                double damage = computeDamage(definition, target, result.getHitPosition());
                target.damage(damage, shooter);
            }
        }

        int remaining = ammo - 1;
        itemFactory.updateAmmoDisplay(weaponItem, remaining, definition.magazineSize());

        state.setLastShotAtMillis(now);
        state.setCurrentSpreadDegrees(Math.min(definition.maxSpreadDegrees(), spreadBeforeShot + definition.growthPerShotDegrees()));

        if (remaining == 0) {
            shooter.sendMessage(color("&eEmpty - reload (swap-hands key)."));
        }
    }

    /** RPG-style heavy ordnance: not hitscan damage against one target - the rocket flies until it hits
     * an entity or a block, then detonates a real vanilla explosion there (breaking terrain for
     * structural breaching, and applying vanilla explosion damage/knockback to everyone in range).
     * "Splash radius" from config is used directly as the explosion's power. */
    private void fireExplosive(Player shooter, World world, Location eyeLocation, Vector direction, WeaponDefinition definition) {
        RayTraceResult entityHit = world.rayTraceEntities(eyeLocation, direction, definition.maxRange(), 0.3,
                candidate -> candidate instanceof LivingEntity && !candidate.equals(shooter));
        RayTraceResult blockHit = world.rayTraceBlocks(eyeLocation, direction, definition.maxRange());

        Location impact;
        if (entityHit != null && entityHit.getHitPosition() != null
                && (blockHit == null || blockHit.getHitPosition() == null
                    || eyeLocation.distance(entityHit.getHitPosition().toLocation(world)) <= eyeLocation.distance(blockHit.getHitPosition().toLocation(world)))) {
            impact = entityHit.getHitPosition().toLocation(world);
        } else if (blockHit != null && blockHit.getHitPosition() != null) {
            impact = blockHit.getHitPosition().toLocation(world);
        } else {
            Vector traveled = direction.clone().multiply(definition.maxRange());
            impact = new Location(world, eyeLocation.getX() + traveled.getX(),
                    eyeLocation.getY() + traveled.getY(), eyeLocation.getZ() + traveled.getZ());
        }

        world.createExplosion(impact, (float) definition.splashRadius(), false, true, shooter);
    }

    private double computeDamage(WeaponDefinition definition, LivingEntity target, Vector hitPosition) {
        if (hitPosition == null) {
            return definition.baseDamage();
        }
        BoundingBox box = target.getBoundingBox();
        double height = box.getHeight();
        if (height <= 0) {
            return definition.baseDamage();
        }
        double relativeHeight = (hitPosition.getY() - box.getMinY()) / height;
        if (relativeHeight >= 0.85) {
            return definition.baseDamage() * definition.headshotMultiplier();
        }
        if (relativeHeight <= 0.25) {
            return definition.baseDamage() * definition.limbMultiplier();
        }
        return definition.baseDamage();
    }

    private Vector applySpread(Vector direction, double maxAngleDegrees) {
        if (maxAngleDegrees <= 0) {
            return direction;
        }
        double maxAngleRad = Math.toRadians(maxAngleDegrees);
        double angle = random.nextDouble() * maxAngleRad;
        double rotation = random.nextDouble() * 2 * Math.PI;

        Vector reference = Math.abs(direction.getY()) < 0.99 ? new Vector(0, 1, 0) : new Vector(1, 0, 0);
        Vector right = cross(direction, reference).normalize();
        Vector up = cross(right, direction).normalize();

        double sinAngle = Math.sin(angle);
        Vector offset = right.multiply(sinAngle * Math.cos(rotation)).add(up.multiply(sinAngle * Math.sin(rotation)));
        return direction.multiply(Math.cos(angle)).add(offset).normalize();
    }

    private static Vector cross(Vector a, Vector b) {
        return new Vector(
                a.getY() * b.getZ() - a.getZ() * b.getY(),
                a.getZ() * b.getX() - a.getX() * b.getZ(),
                a.getX() * b.getY() - a.getY() * b.getX()
        );
    }

    public void reload(Player player, ItemStack weaponItem, WeaponDefinition definition) {
        PlayerWeaponState state = stateFor(player.getUniqueId());
        if (state.isReloading()) {
            player.sendMessage(color("&eAlready reloading."));
            return;
        }
        int ammo = getAmmo(weaponItem);
        if (ammo >= definition.magazineSize()) {
            player.sendMessage(color("&7Already fully loaded."));
            return;
        }
        if (!consumeAmmoItem(player, definition)) {
            player.sendMessage(color("&cNo " + definition.ammoDisplayName() + " &cin your inventory."));
            return;
        }

        state.setReloading(true);
        player.sendMessage(color("&eReloading " + definition.displayName() + "&e..."));

        long delayTicks = Math.round(definition.reloadSeconds() * 20);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            state.setReloading(false);
            ItemStack current = player.getInventory().getItemInMainHand();
            if (identify(current) == definition) {
                itemFactory.updateAmmoDisplay(current, definition.magazineSize(), definition.magazineSize());
                player.sendMessage(color("&aReloaded."));
            }
        }, delayTicks);
    }

    private boolean consumeAmmoItem(Player player, WeaponDefinition definition) {
        PlayerInventory inventory = player.getInventory();
        ItemStack[] contents = inventory.getContents();
        if (contents == null) {
            return false;
        }
        for (int i = 0; i < contents.length; i++) {
            ItemStack stack = contents[i];
            if (stack == null || stack.getType() != definition.ammoMaterial() || stack.getItemMeta() == null) {
                continue;
            }
            String forWeapon = stack.getItemMeta().getPersistentDataContainer().get(keys.ammoForWeaponId, PersistentDataType.STRING);
            if (forWeapon == null || !forWeapon.equalsIgnoreCase(definition.id())) {
                continue;
            }
            if (stack.getAmount() <= 1) {
                inventory.setItem(i, null);
            } else {
                stack.setAmount(stack.getAmount() - 1);
                inventory.setItem(i, stack);
            }
            return true;
        }
        return false;
    }

    private static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
