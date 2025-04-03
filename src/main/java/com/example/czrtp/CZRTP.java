package com.example.czrtp;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class CZRTP extends JavaPlugin {
    private ZoneManager zoneManager;
    private ZoneConfigManager zoneConfigManager;
    private TeleportManager teleportManager;

    @Override
    public void onEnable() {
        // Create config files if they don't exist
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        // Initialize the zone config file first
        saveDefaultConfig(); // For main config.yml

        File zoneFile = new File(getDataFolder(), "zones.yml");
        if (!zoneFile.exists()) {
            saveResource("zones.yml", false);
        }

        // Create default config_zone.yml if it doesn't exist
        File zoneConfigFile = new File(getDataFolder(), "config_zone.yml");
        if (!zoneConfigFile.exists()) {
            // Since we're adding this file now, we need to create it manually
            try (InputStream in = getResource("config_zone.yml")) {
                if (in != null) {
                    Files.copy(in, zoneConfigFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } else {
                    // If the resource doesn't exist in the JAR, create a basic config
                    YamlConfiguration config = new YamlConfiguration();
                    config.set("global.teleport_delay", 3);
                    config.set("global.cooldown", 60);
                    config.set("global.message", "&aTeleporting to zone &6{zone_name}&a in &6{delay}&a seconds...");
                    config.set("global.cooldown_message", "&cYou must wait &6{cooldown}&c seconds before using this zone again.");
                    config.set("global.success_message", "&aYou have been teleported to zone &6{zone_name}&a!");
                    config.save(zoneConfigFile);
                }
            } catch (IOException e) {
                getLogger().severe("Could not create default config_zone.yml!");
                e.printStackTrace();
            }
        }

        // Initialize the managers
        zoneConfigManager = new ZoneConfigManager(this);
        zoneManager = new ZoneManager(this);
        teleportManager = new TeleportManager(this);

        // Register the command executor
        getCommand("czrtp").setExecutor(new CZRTPCommand(this));

        getLogger().info("CZRTP has been enabled!");
    }

    @Override
    public void onDisable() {
        // Save any unsaved data
        if (zoneManager != null) {
            zoneManager.saveZones();
        }

        if (zoneConfigManager != null) {
            zoneConfigManager.saveConfig();
        }

        getLogger().info("CZRTP has been disabled!");
    }

    public ZoneManager getZoneManager() {
        return zoneManager;
    }

    public ZoneConfigManager getZoneConfigManager() {
        return zoneConfigManager;
    }

    public TeleportManager getTeleportManager() {
        return teleportManager;
    }

    public void reload() {
        // Do NOT save the current configuration as this would overwrite manual changes
        // Instead, just reload everything from the files

        // Reload main config.yml
        reloadConfig();

        // Clear instances to force recreation
        zoneManager = null;
        zoneConfigManager = null;
        teleportManager = null;

        // Reload configurations from disk
        zoneConfigManager = new ZoneConfigManager(this);
        zoneManager = new ZoneManager(this);
        teleportManager = new TeleportManager(this);

        getLogger().info("CZRTP configuration reloaded!");
    }
}