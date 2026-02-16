package com.drugs;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StrainsCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can view strains.");
            return true;
        }

        if (!player.hasPermission("drugs.strains")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to view strains.");
            return true;
        }

        StrainsMenuGUI.open(player, 0);
        return true;
    }
}
