package com.example.czrtp;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ZoneConfigManager {
    private final CZRTP plugin;
    private final File configFile;
    private FileConfiguration config;
    private final Map<String, ZoneConfig> zoneConfigs;
    private final Map<UUID, Map<String, Long>> playerCooldowns;

    // Pattern for RGB hex colors in format &x&R&R&G&G&B&B
    private static final Pattern HEX_PATTERN = Pattern.compile("(&x)(&[0-9a-fA-F])(&[0-9a-fA-F])(&[0-9a-fA-F])(&[0-9a-fA-F])(&[0-9a-fA-F])(&[0-9a-fA-F])");

    // Default values
    private static final int DEFAULT_TELEPORT_DELAY = 3;
    private static final int DEFAULT_COOLDOWN = 60;
    private static final String DEFAULT_MESSAGE = "&aTeleporting to zone &6{zone_name}&a in &6{delay}&a seconds...";
    private static final String DEFAULT_COOLDOWN_MESSAGE = "&cYou must wait &6{cooldown}&c seconds before using this zone again.";
    private static final String DEFAULT_SUCCESS_MESSAGE = "&aYou have been teleported to zone &6{zone_name}&a!";
    private static final boolean DEFAULT_PREFIX_ENABLED = true;
    private static final String DEFAULT_PREFIX_TEXT = "[&x&0&0&0&0&0&0&l&oC&x&2&e&2&e&2&e&l&oZ&x&5&c&5&c&5&c&l&oR&x&8&9&8&9&8&9&l&oT&x&b&7&b&7&b&7&l&oP]";

    public ZoneConfigManager(CZRTP plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "config_zone.yml");
        this.zoneConfigs = new HashMap<>();
        this.playerCooldowns = new HashMap<>();
        loadConfig();
    }

    public void loadConfig() {
        if (!configFile.exists()) {
            // Create default config
            plugin.saveResource("config_zone.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(configFile);
        zoneConfigs.clear();

        // Load global defaults
        boolean globalPrefixEnabled = config.getBoolean("global.prefix.enabled", DEFAULT_PREFIX_ENABLED);
        String globalPrefixText = config.getString("global.prefix.text", DEFAULT_PREFIX_TEXT);
        int globalDelay = config.getInt("global.teleport_delay", DEFAULT_TELEPORT_DELAY);
        int globalCooldown = config.getInt("global.cooldown", DEFAULT_COOLDOWN);
        String globalMessage = config.getString("global.message", DEFAULT_MESSAGE);
        String globalCooldownMessage = config.getString("global.cooldown_message", DEFAULT_COOLDOWN_MESSAGE);
        String globalSuccessMessage = config.getString("global.success_message", DEFAULT_SUCCESS_MESSAGE);

        // Load zone-specific configurations
        ConfigurationSection zonesSection = config.getConfigurationSection("zones");
        if (zonesSection != null) {
            for (String zoneName : zonesSection.getKeys(false)) {
                ConfigurationSection zoneSection = zonesSection.getConfigurationSection(zoneName);
                if (zoneSection != null) {
                    // Check if zone has prefix settings, otherwise use global
                    boolean prefixEnabled = globalPrefixEnabled;
                    String prefixText = globalPrefixText;

                    if (zoneSection.contains("prefix")) {
                        ConfigurationSection prefixSection = zoneSection.getConfigurationSection("prefix");
                        if (prefixSection != null) {
                            prefixEnabled = prefixSection.getBoolean("enabled", globalPrefixEnabled);
                            prefixText = prefixSection.getString("text", globalPrefixText);
                        }
                    }

                    int delay = zoneSection.getInt("teleport_delay", globalDelay);
                    int cooldown = zoneSection.getInt("cooldown", globalCooldown);
                    String message = zoneSection.getString("message", globalMessage);
                    String cooldownMessage = zoneSection.getString("cooldown_message", globalCooldownMessage);
                    String successMessage = zoneSection.getString("success_message", globalSuccessMessage);

                    ZoneConfig zoneConfig = new ZoneConfig(
                            delay, cooldown, message, cooldownMessage, successMessage,
                            prefixEnabled, prefixText
                    );
                    zoneConfigs.put(zoneName, zoneConfig);
                }
            }
        }

        // Set global defaults as fallback
        ZoneConfig defaultConfig = new ZoneConfig(
                globalDelay, globalCooldown, globalMessage, globalCooldownMessage,
                globalSuccessMessage, globalPrefixEnabled, globalPrefixText
        );
        zoneConfigs.put("_default", defaultConfig);

        plugin.getLogger().info("Loaded configuration from " + configFile.getName());
    }

    public void saveConfig() {
        // Save global defaults first
        ZoneConfig defaultConfig = zoneConfigs.getOrDefault("_default",
                new ZoneConfig(DEFAULT_TELEPORT_DELAY, DEFAULT_COOLDOWN, DEFAULT_MESSAGE,
                        DEFAULT_COOLDOWN_MESSAGE, DEFAULT_SUCCESS_MESSAGE,
                        DEFAULT_PREFIX_ENABLED, DEFAULT_PREFIX_TEXT));

        config.set("global.prefix.enabled", defaultConfig.isPrefixEnabled());
        config.set("global.prefix.text", defaultConfig.getPrefixText());
        config.set("global.teleport_delay", defaultConfig.getTeleportDelay());
        config.set("global.cooldown", defaultConfig.getCooldown());
        config.set("global.message", defaultConfig.getMessage());
        config.set("global.cooldown_message", defaultConfig.getCooldownMessage());
        config.set("global.success_message", defaultConfig.getSuccessMessage());

        // Save zone-specific configurations
        for (Map.Entry<String, ZoneConfig> entry : zoneConfigs.entrySet()) {
            String zoneName = entry.getKey();
            if (!zoneName.equals("_default")) {
                ZoneConfig zoneConfig = entry.getValue();

                // Only save prefix if it differs from global
                if (zoneConfig.isPrefixEnabled() != defaultConfig.isPrefixEnabled() ||
                        !zoneConfig.getPrefixText().equals(defaultConfig.getPrefixText())) {
                    config.set("zones." + zoneName + ".prefix.enabled", zoneConfig.isPrefixEnabled());
                    config.set("zones." + zoneName + ".prefix.text", zoneConfig.getPrefixText());
                }

                config.set("zones." + zoneName + ".teleport_delay", zoneConfig.getTeleportDelay());
                config.set("zones." + zoneName + ".cooldown", zoneConfig.getCooldown());
                config.set("zones." + zoneName + ".message", zoneConfig.getMessage());
                config.set("zones." + zoneName + ".cooldown_message", zoneConfig.getCooldownMessage());
                config.set("zones." + zoneName + ".success_message", zoneConfig.getSuccessMessage());
            }
        }

        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save config to " + configFile.getName());
            e.printStackTrace();
        }
    }

    public ZoneConfig getZoneConfig(String zoneName) {
        return zoneConfigs.getOrDefault(zoneName, zoneConfigs.get("_default"));
    }

    public void setZoneConfig(String zoneName, ZoneConfig zoneConfig) {
        zoneConfigs.put(zoneName, zoneConfig);
        saveConfig();
    }

    public boolean isOnCooldown(UUID playerUUID, String zoneName) {
        if (!playerCooldowns.containsKey(playerUUID)) {
            return false;
        }

        Map<String, Long> cooldowns = playerCooldowns.get(playerUUID);
        if (!cooldowns.containsKey(zoneName)) {
            return false;
        }

        long lastUsed = cooldowns.get(zoneName);
        ZoneConfig zoneConfig = getZoneConfig(zoneName);
        long cooldownTime = zoneConfig.getCooldown() * 1000L; // Convert to milliseconds

        return System.currentTimeMillis() - lastUsed < cooldownTime;
    }

    public int getRemainingCooldown(UUID playerUUID, String zoneName) {
        if (!playerCooldowns.containsKey(playerUUID)) {
            return 0;
        }

        Map<String, Long> cooldowns = playerCooldowns.get(playerUUID);
        if (!cooldowns.containsKey(zoneName)) {
            return 0;
        }

        long lastUsed = cooldowns.get(zoneName);
        ZoneConfig zoneConfig = getZoneConfig(zoneName);
        long cooldownTime = zoneConfig.getCooldown() * 1000L; // Convert to milliseconds

        long remainingMillis = cooldownTime - (System.currentTimeMillis() - lastUsed);
        if (remainingMillis <= 0) {
            return 0;
        }

        return (int) Math.ceil(remainingMillis / 1000.0); // Convert back to seconds, ceiling to avoid 0
    }

    public void setCooldown(UUID playerUUID, String zoneName) {
        if (!playerCooldowns.containsKey(playerUUID)) {
            playerCooldowns.put(playerUUID, new HashMap<>());
        }

        playerCooldowns.get(playerUUID).put(zoneName, System.currentTimeMillis());
    }

    public String formatMessage(String message, String zoneName, int delay, int cooldown) {
        // Get zone config
        ZoneConfig zoneConfig = getZoneConfig(zoneName);

        // Get prefix
        String prefix = "";
        if (zoneConfig.isPrefixEnabled()) {
            prefix = formatColorCodes(zoneConfig.getPrefixText());
        }

        // Replace placeholders
        String formattedMessage = message
                .replace("{zone_name}", zoneName)
                .replace("{delay}", String.valueOf(delay))
                .replace("{cooldown}", String.valueOf(cooldown))
                .replace("{prefix}", prefix);

        // Translate color codes
        return formatColorCodes(formattedMessage);
    }

    /**
     * Formats both standard color codes and hex color codes
     * @param text Text to format
     * @return Formatted text
     */
    private String formatColorCodes(String text) {
        if (text == null) {
            return "";
        }

        // Process hex colors in format &x&R&R&G&G&B&B
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            StringBuilder hexColor = new StringBuilder("#");
            for (int i = 2; i <= 7; i++) {
                hexColor.append(matcher.group(i).charAt(1));
            }

            try {
                // Convert to chat color (using reflection since ChatColor.of() might not be available in all versions)
                java.lang.reflect.Method method = net.md_5.bungee.api.ChatColor.class.getMethod("of", String.class);
                net.md_5.bungee.api.ChatColor color = (net.md_5.bungee.api.ChatColor) method.invoke(null, hexColor.toString());
                matcher.appendReplacement(sb, color.toString());
            } catch (Exception e) {
                // If reflection fails, just use the original text
                matcher.appendReplacement(sb, matcher.group(0));
            }
        }
        matcher.appendTail(sb);

        // Process standard color codes
        return ChatColor.translateAlternateColorCodes('&', sb.toString());
    }
}

// Inner class for zone configuration
class ZoneConfig {
    private int teleportDelay;
    private int cooldown;
    private String message;
    private String cooldownMessage;
    private String successMessage;
    private boolean prefixEnabled;
    private String prefixText;

    public ZoneConfig(int teleportDelay, int cooldown, String message, String cooldownMessage,
                      String successMessage, boolean prefixEnabled, String prefixText) {
        this.teleportDelay = teleportDelay;
        this.cooldown = cooldown;
        this.message = message;
        this.cooldownMessage = cooldownMessage;
        this.successMessage = successMessage;
        this.prefixEnabled = prefixEnabled;
        this.prefixText = prefixText;
    }

    public int getTeleportDelay() {
        return teleportDelay;
    }

    public void setTeleportDelay(int teleportDelay) {
        this.teleportDelay = teleportDelay;
    }

    public int getCooldown() {
        return cooldown;
    }

    public void setCooldown(int cooldown) {
        this.cooldown = cooldown;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCooldownMessage() {
        return cooldownMessage;
    }

    public void setCooldownMessage(String cooldownMessage) {
        this.cooldownMessage = cooldownMessage;
    }

    public String getSuccessMessage() {
        return successMessage;
    }

    public void setSuccessMessage(String successMessage) {
        this.successMessage = successMessage;
    }

    public boolean isPrefixEnabled() {
        return prefixEnabled;
    }

    public void setPrefixEnabled(boolean prefixEnabled) {
        this.prefixEnabled = prefixEnabled;
    }

    public String getPrefixText() {
        return prefixText;
    }

    public void setPrefixText(String prefixText) {
        this.prefixText = prefixText;
    }
}