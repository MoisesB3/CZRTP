package com.example.czrtp;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeleportManager {
    private final CZRTP plugin;
    private final Map<UUID, Integer> pendingTeleports;

    public TeleportManager(CZRTP plugin) {
        this.plugin = plugin;
        this.pendingTeleports = new HashMap<>();
    }

    public boolean teleportPlayer(Player player, String zoneName) {
        ZoneManager zoneManager = plugin.getZoneManager();
        ZoneConfigManager configManager = plugin.getZoneConfigManager();

        // Check if the zone exists
        Zone zone = zoneManager.getZone(zoneName);
        if (zone == null || !zone.isComplete()) {
            player.sendMessage("§cZone '" + zoneName + "' doesn't exist or is incomplete.");
            return false;
        }

        UUID playerUUID = player.getUniqueId();

        // Check if the player is already being teleported
        if (pendingTeleports.containsKey(playerUUID)) {
            player.sendMessage("§cYou are already being teleported.");
            return false;
        }

        // Check cooldown - Skip cooldown check for admins teleporting others
        boolean isAdminTeleport = !player.equals(plugin.getServer().getPlayer(playerUUID));
        if (!isAdminTeleport && configManager.isOnCooldown(playerUUID, zoneName)) {
            int remaining = configManager.getRemainingCooldown(playerUUID, zoneName);
            ZoneConfig zoneConfig = configManager.getZoneConfig(zoneName);
            String cooldownMessage = configManager.formatMessage(
                    zoneConfig.getCooldownMessage(),
                    zoneName,
                    zoneConfig.getTeleportDelay(),
                    remaining
            );
            player.sendMessage(cooldownMessage);
            return false;
        }

        // Get teleport delay
        ZoneConfig zoneConfig = configManager.getZoneConfig(zoneName);
        int delay = zoneConfig.getTeleportDelay();

        // Send initial message
        if (delay > 0) {
            String message = configManager.formatMessage(
                    zoneConfig.getMessage(),
                    zoneName,
                    delay,
                    zoneConfig.getCooldown()
            );
            player.sendMessage(message);
        }

        // Schedule teleport
        int taskId = new BukkitRunnable() {
            @Override
            public void run() {
                // Get random location in zone
                Location location = zoneManager.getRandomLocationInZone(zoneName);

                if (location == null) {
                    player.sendMessage("§cCouldn't find a safe location in zone '" + zoneName + "'.");
                    pendingTeleports.remove(playerUUID);
                    return;
                }

                // Teleport player
                player.teleport(location);

                // Send success message
                String successMessage = configManager.formatMessage(
                        zoneConfig.getSuccessMessage(),
                        zoneName,
                        delay,
                        zoneConfig.getCooldown()
                );
                player.sendMessage(successMessage);

                // Set cooldown - Skip setting cooldown for admin teleports
                if (!isAdminTeleport) {
                    configManager.setCooldown(playerUUID, zoneName);
                }

                // Remove from pending teleports
                pendingTeleports.remove(playerUUID);
            }
        }.runTaskLater(plugin, delay * 20L).getTaskId();

        pendingTeleports.put(playerUUID, taskId);
        return true;
    }

    public boolean cancelTeleport(Player player) {
        UUID playerUUID = player.getUniqueId();

        if (pendingTeleports.containsKey(playerUUID)) {
            int taskId = pendingTeleports.get(playerUUID);
            plugin.getServer().getScheduler().cancelTask(taskId);
            pendingTeleports.remove(playerUUID);
            player.sendMessage("§aTeleport cancelled.");
            return true;
        }

        player.sendMessage("§cYou don't have any pending teleports.");
        return false;
    }
}