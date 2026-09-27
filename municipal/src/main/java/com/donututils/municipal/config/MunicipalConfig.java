package com.donututils.municipal.config;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Immutable snapshot of config.yml, re-read on reload. */
public record MunicipalConfig(
        Map<String, PermitDefinition> permits,
        Set<String> cityLimitWorlds,
        Set<String> controlledWeapons,
        String jailWorld,
        double jailX,
        double jailY,
        double jailZ,
        double jailRadius,
        Set<String> allowedCommandsWhileJailed,
        int releaseCheckSeconds,
        int maxSentenceMinutes
) {

    public PermitDefinition permit(String type) {
        return permits.get(type.toLowerCase(Locale.ROOT));
    }

    public boolean isCityLimitWorld(String worldName) {
        return cityLimitWorlds.contains(worldName);
    }

    public boolean isControlledWeapon(String materialName) {
        return controlledWeapons.contains(materialName.toUpperCase(Locale.ROOT));
    }

    public boolean isCommandAllowedWhileJailed(String commandLabel) {
        return allowedCommandsWhileJailed.contains(commandLabel.toLowerCase(Locale.ROOT));
    }
}
