package com.example.czrtp;

import org.bukkit.Location;
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
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        // Require permission
        if (!player.hasPermission("czrtp.use")) {
            player.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sendHelpMessage(player);
            return true;
        }

        ZoneManager zoneManager = plugin.getZoneManager();

        // Handle create command
        if (args.length >= 2 && args[0].equalsIgnoreCase("create")) {
            String zoneName = args[1];
            return zoneManager.createZone(player, zoneName);
        }

        // Handle teleport command
        if (args.length == 2 && args[0].equalsIgnoreCase("tp")) {
            String zoneName = args[1];
            Location location = zoneManager.getRandomLocationInZone(zoneName);

            if (location == null) {
                player.sendMessage("§cZone '" + zoneName + "' doesn't exist or is incomplete.");
                return true;
            }

            player.teleport(location);
            player.sendMessage("§aTeleported to a random location in zone '" + zoneName + "'.");
            return true;
        }

        // Handle position setting
        if (args.length == 2) {
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

        sendHelpMessage(player);
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
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = Arrays.asList("create", "tp");
            String input = args[0].toLowerCase();
            return completions.stream()
                    .filter(s -> s.startsWith(input))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}