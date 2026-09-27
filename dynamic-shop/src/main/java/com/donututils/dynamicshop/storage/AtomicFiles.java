package com.donututils.dynamicshop.storage;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/** Write-to-temp-then-rename helper so a crash or power loss mid-save can never leave a half-written,
 * corrupted YAML file behind - the rename is the only moment the old file is replaced. */
public final class AtomicFiles {

    private AtomicFiles() {
    }

    public static void writeYaml(YamlConfiguration yaml, File target) throws IOException {
        File parent = target.getParentFile();
        if (parent != null) {
            Files.createDirectories(parent.toPath());
        }
        File temp = new File(parent, target.getName() + ".tmp");
        yaml.save(temp);
        try {
            Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
