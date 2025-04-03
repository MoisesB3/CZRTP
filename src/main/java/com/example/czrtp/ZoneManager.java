package com.example.czrtp;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class ZoneManager {
    private final CZRTP plugin;
    private final File zoneFile;
    private FileConfiguration zoneConfig;
    private final Map<String, Zone> zones;
    private final Map<UUID, ZoneCreation> pendingZones;
    private final Random random;

    public ZoneManager(CZRTP plugin) {
        this.plugin = plugin;
        this.zoneFile = new File(plugin.getDataFolder(), "zones.yml");
        this.zones = new HashMap<>();
        this.pendingZones = new HashMap<>();
        this.random = new Random();
        loadZones();
    }

    public void loadZones() {
        zoneConfig = YamlConfiguration.loadConfiguration(zoneFile);
        zones.clear();

        ConfigurationSection zonesSection = zoneConfig.getConfigurationSection("zones");
        if (zonesSection != null) {
            for (String zoneName : zonesSection.getKeys(false)) {
                ConfigurationSection zoneSection = zonesSection.getConfigurationSection(zoneName);
                if (zoneSection != null) {
                    Zone zone = new Zone(zoneName);

                    // Load positions
                    for (int i = 1; i <= 4; i++) {
                        ConfigurationSection posSection = zoneSection.getConfigurationSection("pos" + i);
                        if (posSection != null) {
                            double x = posSection.getDouble("x");
                            double y = posSection.getDouble("y");
                            double z = posSection.getDouble("z");
                            String worldName = posSection.getString("world");

                            zone.setPosition(i, new Position(x, y, z, worldName));
                        }
                    }

                    zones.put(zoneName, zone);
                }
            }
        }
    }

    public void saveZones() {
        // Clear the existing config
        zoneConfig.set("zones", null);

        // Save all zones
        for (Map.Entry<String, Zone> entry : zones.entrySet()) {
            String zoneName = entry.getKey();
            Zone zone = entry.getValue();

            for (int i = 1; i <= 4; i++) {
                Position pos = zone.getPosition(i);
                if (pos != null) {
                    zoneConfig.set("zones." + zoneName + ".pos" + i + ".x", pos.getX());
                    zoneConfig.set("zones." + zoneName + ".pos" + i + ".y", pos.getY());
                    zoneConfig.set("zones." + zoneName + ".pos" + i + ".z", pos.getZ());
                    zoneConfig.set("zones." + zoneName + ".pos" + i + ".world", pos.getWorldName());
                }
            }
        }

        try {
            zoneConfig.save(zoneFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save zones to " + zoneFile.getName());
            e.printStackTrace();
        }
    }

    public boolean createZone(Player player, String zoneName) {
        if (zones.containsKey(zoneName)) {
            player.sendMessage("§cA zone with that name already exists!");
            return false;
        }

        pendingZones.put(player.getUniqueId(), new ZoneCreation(zoneName));
        player.sendMessage("§aZone creation started for '" + zoneName + "'.");
        player.sendMessage("§aUse /czrtp " + zoneName + " pos1, pos2, pos3, and pos4 to define the zone boundaries.");
        return true;
    }

    public boolean setPosition(Player player, String zoneName, int posNumber) {
        UUID playerUUID = player.getUniqueId();

        // Check if player is creating this zone
        if (pendingZones.containsKey(playerUUID) && pendingZones.get(playerUUID).getZoneName().equals(zoneName)) {
            ZoneCreation zoneCreation = pendingZones.get(playerUUID);
            Location loc = player.getLocation();

            zoneCreation.setPosition(posNumber, new Position(
                    loc.getX(), loc.getY(), loc.getZ(), loc.getWorld().getName()
            ));

            player.sendMessage("§aPosition " + posNumber + " set for zone '" + zoneName + "'.");

            // Check if all positions are set
            if (zoneCreation.isComplete()) {
                finalizeZone(player, zoneCreation);
                return true;
            }

            return true;
        } else if (zones.containsKey(zoneName)) {
            player.sendMessage("§cThis zone already exists! Use a different command to modify existing zones.");
            return false;
        } else {
            player.sendMessage("§cYou need to start zone creation with /czrtp create " + zoneName + " first!");
            return false;
        }
    }

    private void finalizeZone(Player player, ZoneCreation zoneCreation) {
        String zoneName = zoneCreation.getZoneName();
        Zone zone = new Zone(zoneName);

        for (int i = 1; i <= 4; i++) {
            zone.setPosition(i, zoneCreation.getPosition(i));
        }

        zones.put(zoneName, zone);
        pendingZones.remove(player.getUniqueId());

        saveZones();

        player.sendMessage("§aZone '" + zoneName + "' has been created and saved successfully!");
    }

    public Zone getZone(String zoneName) {
        return zones.get(zoneName);
    }

    public Location getRandomLocationInZone(String zoneName) {
        Zone zone = zones.get(zoneName);
        if (zone == null || !zone.isComplete()) {
            return null;
        }

        // Get all four positions
        Position pos1 = zone.getPosition(1);
        Position pos2 = zone.getPosition(2);
        Position pos3 = zone.getPosition(3);
        Position pos4 = zone.getPosition(4);

        // Simple implementation - assuming quadrilateral area between points
        double minX = Math.min(Math.min(Math.min(pos1.getX(), pos2.getX()), pos3.getX()), pos4.getX());
        double maxX = Math.max(Math.max(Math.max(pos1.getX(), pos2.getX()), pos3.getX()), pos4.getX());
        double minZ = Math.min(Math.min(Math.min(pos1.getZ(), pos2.getZ()), pos3.getZ()), pos4.getZ());
        double maxZ = Math.max(Math.max(Math.max(pos1.getZ(), pos2.getZ()), pos3.getZ()), pos4.getZ());

        // Use the world from pos1
        World world = plugin.getServer().getWorld(pos1.getWorldName());
        if (world == null) {
            return null;
        }

        // Get max height of the world
        int maxHeight = world.getMaxHeight();
        int minHeight = world.getMinHeight();

        // Maximum number of attempts to find a safe location
        int maxAttempts = 50;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            // Generate random coordinates within the bounds
            double x = minX + (maxX - minX) * random.nextDouble();
            double z = minZ + (maxZ - minZ) * random.nextDouble();

            // Find a safe location
            Location safeLocation = findSafeLocation(world, x, z, minHeight, maxHeight);
            if (safeLocation != null) {
                return safeLocation;
            }
        }

        // If we couldn't find a safe location after max attempts, use the center point of the zone
        // and try to find a safe Y coordinate
        double centerX = (minX + maxX) / 2;
        double centerZ = (minZ + maxZ) / 2;

        return findSafeLocation(world, centerX, centerZ, minHeight, maxHeight);
    }

    private Location findSafeLocation(World world, double x, double z, int minHeight, int maxHeight) {
        // Start from the top and work down to find the highest non-air block
        for (int y = maxHeight - 1; y >= minHeight; y--) {
            Block block = world.getBlockAt((int) x, y, (int) z);
            Block blockAbove = world.getBlockAt((int) x, y + 1, (int) z);
            Block blockTwoAbove = world.getBlockAt((int) x, y + 2, (int) z);

            // Check if this is a safe spot (solid block with two air blocks above it)
            if (isSolid(block) && !isSolid(blockAbove) && !isSolid(blockTwoAbove)) {
                // Add 1 to y to place the player on top of the block
                return new Location(world, x, y + 1, z);
            }
        }

        return null;
    }

    private boolean isSolid(Block block) {
        return block.getType().isSolid() && !block.isLiquid();
    }
}