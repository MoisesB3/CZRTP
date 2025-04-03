package com.example.czrtp;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CZRTPCommand implements CommandExecutor, TabCompleter {
    private final CZRTP plugin;

    public CZRTPCommand(CZRTP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Allow console to use the command for teleporting others
        boolean isConsole = !(sender instanceof Player);

        if (isConsole && (args.length < 3 || !args[0].equalsIgnoreCase("tp"))) {
            sender.sendMessage("§cConsole can only use the command to teleport others: /czrtp tp <zone_name> <player_name>");
            return true;
        }

        Player player = isConsole ? null : (Player) sender;

        // Check for reload command first (admin permission)
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!isConsole && !player.hasPermission("czrtp.admin")) {
                sender.sendMessage("§cYou don't have permission to reload the plugin.");
                return true;
            }

            plugin.reload();
            sender.sendMessage("§aPlugin configuration reloaded!");
            return true;
        }

        // Require basic permission for other commands
        if (!isConsole && !player.hasPermission("czrtp.use")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            if (!isConsole) {
                sendHelpMessage(player);
            }
            return true;
        }

        ZoneManager zoneManager = plugin.getZoneManager();
        ZoneConfigManager configManager = plugin.getZoneConfigManager();
        TeleportManager teleportManager = plugin.getTeleportManager();

        // Handle cancel command
        if (args.length == 1 && args[0].equalsIgnoreCase("cancel") && !isConsole) {
            return teleportManager.cancelTeleport(player);
        }

        // Handle create command
        if (args.length >= 2 && args[0].equalsIgnoreCase("create") && !isConsole) {
            String zoneName = args[1];
            return zoneManager.createZone(player, zoneName);
        }

        // Handle teleport command
        if (args[0].equalsIgnoreCase("tp")) {
            // Basic teleport: /czrtp tp <zone_name>
            if (args.length == 2 && !isConsole) {
                String zoneName = args[1];

                // Check if zone exists
                if (zoneManager.getZone(zoneName) == null) {
                    sender.sendMessage("§cZone '" + zoneName + "' doesn't exist.");
                    return true;
                }

                // Check zone-specific permission
                if (!player.hasPermission("czrtp.admin") &&
                        !player.hasPermission("czrtp." + zoneName.toLowerCase()) &&
                        !player.hasPermission("czrtp.*")) {

                    player.sendMessage("§cYou don't have permission to teleport to zone '" + zoneName + "'.");
                    return true;
                }

                return teleportManager.teleportPlayer(player, zoneName);
            }
            // Teleport others: /czrtp tp <zone_name> <player_name>
            else if (args.length == 3) {
                // Check for admin permission if not console
                if (!isConsole && !sender.hasPermission("czrtp.admin")) {
                    sender.sendMessage("§cYou don't have permission to teleport other players.");
                    return true;
                }

                String zoneName = args[1];

                // Check if zone exists
                if (zoneManager.getZone(zoneName) == null) {
                    sender.sendMessage("§cZone '" + zoneName + "' doesn't exist.");
                    return true;
                }

                String targetPlayerName = args[2];

                // Find target player
                Player targetPlayer = plugin.getServer().getPlayer(targetPlayerName);
                if (targetPlayer == null) {
                    sender.sendMessage("§cPlayer '" + targetPlayerName + "' is not online.");
                    return true;
                }

                // Teleport the target player
                boolean success = teleportManager.teleportPlayer(targetPlayer, zoneName);

                // Notify the command sender
                if (success) {
                    sender.sendMessage("§aTeleporting " + targetPlayer.getName() + " to zone '" + zoneName + "'.");
                }

                return success;
            } else {
                sender.sendMessage("§cUsage: /czrtp tp <zone_name> [player_name]");
                return true;
            }
        }

        // Handle options command
        if (args.length >= 3 && args[0].equalsIgnoreCase("options")) {
            if (!isConsole && !player.hasPermission("czrtp.admin")) {
                sender.sendMessage("§cYou don't have permission to change zone options.");
                return true;
            }

            String zoneName = args[1];
            String option = args[2].toLowerCase();

            // Check if the zone exists for specific zones (not global defaults)
            if (!zoneName.equalsIgnoreCase("global") && zoneManager.getZone(zoneName) == null) {
                sender.sendMessage("§cZone '" + zoneName + "' doesn't exist.");
                return true;
            }

            // Get the config (global or zone-specific)
            String configKey = zoneName.equalsIgnoreCase("global") ? "_default" : zoneName;
            ZoneConfig zoneConfig = configManager.getZoneConfig(configKey);

            // Check for option value
            if (args.length < 4) {
                sender.sendMessage("§cYou need to provide a value for the option.");
                return true;
            }

            // Handle different options
            if (option.equals("delay")) {
                try {
                    int delay = Integer.parseInt(args[3]);
                    if (delay < 0) {
                        sender.sendMessage("§cDelay must be a positive number.");
                        return true;
                    }

                    zoneConfig.setTeleportDelay(delay);
                    configManager.setZoneConfig(configKey, zoneConfig);
                    sender.sendMessage("§aSet teleport delay for " +
                            (zoneName.equalsIgnoreCase("global") ? "global default" : "zone '" + zoneName + "'") +
                            " to " + delay + " seconds.");
                    return true;
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cDelay must be a number.");
                    return true;
                }
            } else if (option.equals("cooldown")) {
                try {
                    int cooldown = Integer.parseInt(args[3]);
                    if (cooldown < 0) {
                        sender.sendMessage("§cCooldown must be a positive number.");
                        return true;
                    }

                    zoneConfig.setCooldown(cooldown);
                    configManager.setZoneConfig(configKey, zoneConfig);
                    sender.sendMessage("§aSet cooldown for " +
                            (zoneName.equalsIgnoreCase("global") ? "global default" : "zone '" + zoneName + "'") +
                            " to " + cooldown + " seconds.");
                    return true;
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cCooldown must be a number.");
                    return true;
                }
            } else if (option.equals("message")) {
                // Combine remaining args for the message
                StringBuilder message = new StringBuilder();
                for (int i = 3; i < args.length; i++) {
                    if (i > 3) message.append(" ");
                    message.append(args[i]);
                }

                zoneConfig.setMessage(message.toString());
                configManager.setZoneConfig(configKey, zoneConfig);
                sender.sendMessage("§aSet teleport message for " +
                        (zoneName.equalsIgnoreCase("global") ? "global default" : "zone '" + zoneName + "'") +
                        " to: " + configManager.formatMessage(message.toString(), zoneName,
                        zoneConfig.getTeleportDelay(), zoneConfig.getCooldown()));
                return true;
            } else if (option.equals("cooldown-message")) {
                // Combine remaining args for the message
                StringBuilder message = new StringBuilder();
                for (int i = 3; i < args.length; i++) {
                    if (i > 3) message.append(" ");
                    message.append(args[i]);
                }

                zoneConfig.setCooldownMessage(message.toString());
                configManager.setZoneConfig(configKey, zoneConfig);
                sender.sendMessage("§aSet cooldown message for " +
                        (zoneName.equalsIgnoreCase("global") ? "global default" : "zone '" + zoneName + "'") +
                        " to: " + configManager.formatMessage(message.toString(), zoneName,
                        zoneConfig.getTeleportDelay(), zoneConfig.getCooldown()));
                return true;
            } else if (option.equals("success-message")) {
                // Combine remaining args for the message
                StringBuilder message = new StringBuilder();
                for (int i = 3; i < args.length; i++) {
                    if (i > 3) message.append(" ");
                    message.append(args[i]);
                }

                zoneConfig.setSuccessMessage(message.toString());
                configManager.setZoneConfig(configKey, zoneConfig);
                sender.sendMessage("§aSet success message for " +
                        (zoneName.equalsIgnoreCase("global") ? "global default" : "zone '" + zoneName + "'") +
                        " to: " + configManager.formatMessage(message.toString(), zoneName,
                        zoneConfig.getTeleportDelay(), zoneConfig.getCooldown()));
                return true;
            }
            // PREFIX OPTIONS
            else if (option.equals("prefix-enabled")) {
                if (args.length < 4) {
                    sender.sendMessage("§cUsage: /czrtp options <zone_name/global> prefix-enabled <true/false>");
                    return true;
                }

                boolean enabled;
                if (args[3].equalsIgnoreCase("true")) {
                    enabled = true;
                } else if (args[3].equalsIgnoreCase("false")) {
                    enabled = false;
                } else {
                    sender.sendMessage("§cValue must be 'true' or 'false'");
                    return true;
                }

                zoneConfig.setPrefixEnabled(enabled);
                configManager.setZoneConfig(configKey, zoneConfig);
                sender.sendMessage("§aSet prefix enabled for " +
                        (zoneName.equalsIgnoreCase("global") ? "global default" : "zone '" + zoneName + "'") +
                        " to " + enabled);
                return true;
            } else if (option.equals("prefix-text")) {
                if (args.length < 4) {
                    sender.sendMessage("§cYou need to provide text for the prefix.");
                    return true;
                }

                // Combine remaining args for the prefix text
                StringBuilder prefixText = new StringBuilder();
                for (int i = 3; i < args.length; i++) {
                    if (i > 3) prefixText.append(" ");
                    prefixText.append(args[i]);
                }

                zoneConfig.setPrefixText(prefixText.toString());
                configManager.setZoneConfig(configKey, zoneConfig);

                // Show the formatted prefix to the player
                String formattedPrefix = configManager.formatMessage("{prefix}", zoneName,
                        zoneConfig.getTeleportDelay(), zoneConfig.getCooldown());

                sender.sendMessage("§aSet prefix text for " +
                        (zoneName.equalsIgnoreCase("global") ? "global default" : "zone '" + zoneName + "'") +
                        " to: " + formattedPrefix);
                return true;
            }
            else {
                sender.sendMessage("§cUnknown option: " + option);
                sender.sendMessage("§7Available options: delay, cooldown, message, cooldown-message, success-message, prefix-enabled, prefix-text");
                return true;
            }
        }

        // Handle position setting (only for players)
        if (args.length == 2 && !isConsole) {
            String zoneName = args[0];
            String posArg = args[1].toLowerCase();

            if (posArg.startsWith("pos")) {
                try {
                    int posNumber = Integer.parseInt(posArg.substring(3));
                    if (posNumber >= 1 && posNumber <= 4) {
                        return zoneManager.setPosition(player, zoneName, posNumber);
                    }
                } catch (NumberFormatException e) {
                    // Ignore parsing errors
                }
            }
        }

        if (!isConsole) {
            sendHelpMessage(player);
        }
        return true;
    }

    private void sendHelpMessage(Player player) {
        player.sendMessage("§6===== CZRTP Commands =====");
        player.sendMessage("§e/czrtp create <zone_name> §7- Start creating a new zone");
        player.sendMessage("§e/czrtp <zone_name> pos1 §7- Set position 1 for the zone");
        player.sendMessage("§e/czrtp <zone_name> pos2 §7- Set position 2 for the zone");
        player.sendMessage("§e/czrtp <zone_name> pos3 §7- Set position 3 for the zone");
        player.sendMessage("§e/czrtp <zone_name> pos4 §7- Set position 4 for the zone");
        player.sendMessage("§e/czrtp tp <zone_name> §7- Teleport to a random location in the zone");
        player.sendMessage("§e/czrtp tp <zone_name> <player_name> §7- Teleport another player to a zone");
        player.sendMessage("§e/czrtp cancel §7- Cancel a pending teleport");

        if (player.hasPermission("czrtp.admin")) {
            player.sendMessage("§6===== Admin Commands =====");
            player.sendMessage("§e/czrtp options <zone_name/global> delay <seconds> §7- Set teleport delay");
            player.sendMessage("§e/czrtp options <zone_name/global> cooldown <seconds> §7- Set cooldown time");
            player.sendMessage("§e/czrtp options <zone_name/global> message <text> §7- Set teleport message");
            player.sendMessage("§e/czrtp options <zone_name/global> cooldown-message <text> §7- Set cooldown message");
            player.sendMessage("§e/czrtp options <zone_name/global> success-message <text> §7- Set success message");
            player.sendMessage("§e/czrtp options <zone_name/global> prefix-enabled <true/false> §7- Enable/disable prefix");
            player.sendMessage("§e/czrtp options <zone_name/global> prefix-text <text> §7- Set prefix text");
            player.sendMessage("§e/czrtp reload §7- Reload plugin configuration");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            // Special case for console tab completion - only show tp command and then zones
            if (args.length == 1) {
                return Arrays.asList("tp").stream()
                        .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                        .collect(Collectors.toList());
            } else if (args.length == 2 && args[0].equalsIgnoreCase("tp")) {
                ZoneManager zoneManager = plugin.getZoneManager();
                List<String> zoneNames = zoneManager.getAllZones().stream()
                        .map(Zone::getName)
                        .collect(Collectors.toList());

                String input = args[1].toLowerCase();
                return zoneNames.stream()
                        .filter(s -> s.toLowerCase().startsWith(input))
                        .collect(Collectors.toList());
            } else if (args.length == 3 && args[0].equalsIgnoreCase("tp")) {
                // For console, suggest online players
                String input = args[2].toLowerCase();
                return plugin.getServer().getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(s -> s.toLowerCase().startsWith(input))
                        .collect(Collectors.toList());
            }
            return new ArrayList<>();
        }

        Player player = (Player) sender;

        if (args.length == 1) {
            List<String> completions = new ArrayList<>(Arrays.asList("create", "tp", "cancel"));

            // Add admin commands if player has permission
            if (player.hasPermission("czrtp.admin")) {
                completions.add("options");
                completions.add("reload");
            }

            // Add zone names for position setting
            ZoneManager zoneManager = plugin.getZoneManager();
            for (Zone zone : zoneManager.getAllZones()) {
                completions.add(zone.getName());
            }

            String input = args[0].toLowerCase();
            return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(input))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("tp")) {
                // Suggest zone names for teleporting - only show zones the player has permission for
                ZoneManager zoneManager = plugin.getZoneManager();
                List<String> zoneNames = zoneManager.getAllZones().stream()
                        .map(Zone::getName)
                        .filter(zoneName -> player.hasPermission("czrtp.admin") ||
                                player.hasPermission("czrtp." + zoneName.toLowerCase()) ||
                                player.hasPermission("czrtp.*"))
                        .collect(Collectors.toList());

                String input = args[1].toLowerCase();
                return zoneNames.stream()
                        .filter(s -> s.toLowerCase().startsWith(input))
                        .collect(Collectors.toList());
            } else if (args[0].equalsIgnoreCase("options") && player.hasPermission("czrtp.admin")) {
                // Suggest zone names or global for options
                ZoneManager zoneManager = plugin.getZoneManager();
                List<String> options = new ArrayList<>();
                options.add("global");

                for (Zone zone : zoneManager.getAllZones()) {
                    options.add(zone.getName());
                }

                String input = args[1].toLowerCase();
                return options.stream()
                        .filter(s -> s.toLowerCase().startsWith(input))
                        .collect(Collectors.toList());
            } else if (!args[0].equalsIgnoreCase("create") && !args[0].equalsIgnoreCase("cancel")) {
                // Position setting completion
                List<String> positions = Arrays.asList("pos1", "pos2", "pos3", "pos4");
                String input = args[1].toLowerCase();
                return positions.stream()
                        .filter(s -> s.startsWith(input))
                        .collect(Collectors.toList());
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("options") && player.hasPermission("czrtp.admin")) {
            // Option type completion
            List<String> options = Arrays.asList("delay", "cooldown", "message", "cooldown-message",
                    "success-message", "prefix-enabled", "prefix-text");
            String input = args[2].toLowerCase();
            return options.stream()
                    .filter(s -> s.startsWith(input))
                    .collect(Collectors.toList());
        } else if (args.length == 3 && args[0].equalsIgnoreCase("tp") && player.hasPermission("czrtp.admin")) {
            // Suggest online player names for teleporting others
            String input = args[2].toLowerCase();
            return plugin.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(s -> s.toLowerCase().startsWith(input))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}