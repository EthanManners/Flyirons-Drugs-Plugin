package com.drugs;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class StrainsCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
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

        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            if (!sender.hasPermission("drugs.admin.strains.give")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use this subcommand.");
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
                return true;
            }

            String strainId = args[2].toLowerCase(Locale.ROOT);
            StrainProfile strain = StrainConfigLoader.getStrain(strainId);
            if (strain == null || !strain.getId().equalsIgnoreCase(strainId)) {
                sender.sendMessage(ChatColor.RED + "Unknown strain id: " + strainId);
                return true;
            }

            ItemStack item = CannabisPlantListener.createStrainFern(strain.getId());
            target.getInventory().addItem(item);
            sender.sendMessage(ChatColor.GREEN + "Gave " + target.getName() + " a " + strain.getDisplayName() + " strain plant.");
            if (!sender.getName().equalsIgnoreCase(target.getName())) {
                target.sendMessage(ChatColor.GREEN + "You received a " + strain.getDisplayName() + " strain plant.");
            }
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " or /" + label + " give <player> <strain-id>");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            if (!sender.hasPermission("drugs.admin.strains.give")) {
                return Collections.emptyList();
            }
            return filter(List.of("give"), args[0]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            if (!sender.hasPermission("drugs.admin.strains.give")) {
                return Collections.emptyList();
            }
            List<String> names = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) {
                names.add(online.getName());
            }
            return filter(names, args[1]);
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            if (!sender.hasPermission("drugs.admin.strains.give")) {
                return Collections.emptyList();
            }
            List<String> strains = new ArrayList<>();
            for (StrainProfile profile : StrainConfigLoader.getAllStrains()) {
                strains.add(profile.getId());
            }
            Collections.sort(strains);
            return filter(strains, args[2]);
        }

        return Collections.emptyList();
    }

    private List<String> filter(List<String> source, String token) {
        String lowered = token == null ? "" : token.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String value : source) {
            if (value.toLowerCase(Locale.ROOT).startsWith(lowered)) {
                out.add(value);
            }
        }
        return out;
    }
}
