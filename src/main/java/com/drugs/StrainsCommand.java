package com.drugs;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

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

        Collection<StrainProfile> all = StrainConfigLoader.getAllStrains();
        int size = Math.max(9, ((all.size() + 8) / 9) * 9);
        Inventory gui = Bukkit.createInventory(null, Math.min(size, 54), ChatColor.DARK_GREEN + "Cannabis Strains");

        List<StrainProfile> sorted = new ArrayList<>(all);
        sorted.sort(Comparator.comparing(StrainProfile::getRarity).thenComparing(StrainProfile::getDisplayName));

        for (StrainProfile strain : sorted) {
            ItemStack item = new ItemStack(Material.FERN);
            ItemMeta meta = item.getItemMeta();
            if (meta == null) continue;

            meta.setDisplayName(ChatColor.GREEN + strain.getDisplayName());
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Rarity: " + ChatColor.YELLOW + capitalize(strain.getRarity()));
            lore.add(ChatColor.GRAY + "Duration Modifier: " + ChatColor.AQUA + String.format("x%.2f", strain.getDurationMultiplier()));
            lore.add(ChatColor.GRAY + "Amplifier Modifier: " + ChatColor.AQUA + String.format("x%.2f", strain.getAmplifierMultiplier()));
            lore.add(ChatColor.GRAY + "Mutation Chance: " + ChatColor.WHITE + String.format("%.2f%%", strain.getMutationChance() * 100));
            lore.add(ChatColor.DARK_GRAY + "Mutates Into:");
            strain.getMutationWeights().entrySet().stream()
                    .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                    .limit(4)
                    .forEach(e -> lore.add(ChatColor.GRAY + " - " + e.getKey() + " (" + e.getValue() + ")"));
            meta.setLore(lore);
            DrugItemMetadata.setItemType(meta, "cannabis_plant");
            DrugItemMetadata.setStrainId(meta, strain.getId());

            item.setItemMeta(meta);
            gui.addItem(item);
        }

        player.openInventory(gui);
        return true;
    }

    private String capitalize(String value) {
        if (value == null || value.isEmpty()) return "Unknown";
        return Character.toUpperCase(value.charAt(0)) + value.substring(1).toLowerCase();
    }
}
