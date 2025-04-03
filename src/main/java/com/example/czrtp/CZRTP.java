package com.example.czrtp;

import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;

public class CZRTP extends JavaPlugin {
    private ZoneManager zoneManager;

    @Override
    public void onEnable() {
        // Create config files if they don't exist
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        File zoneFile = new File(getDataFolder(), "zones.yml");
        if (!zoneFile.exists()) {
            saveResource("zones.yml", false);
        }

        // Initialize the zone manager
        zoneManager = new ZoneManager(this);

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

        getLogger().info("CZRTP has been disabled!");
    }

    public ZoneManager getZoneManager() {
        return zoneManager;
    }
}