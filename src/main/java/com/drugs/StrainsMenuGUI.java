package com.drugs;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Paginated strains GUI grouped by rarity tier.
 */
public final class StrainsMenuGUI {

    private static final String TITLE_PREFIX = ChatColor.DARK_GREEN + "Strains";
    private static final List<String> RARITY_ORDER = List.of("common", "uncommon", "rare", "legendary");
    private static final int GUI_SIZE = 54;
    private static final int ITEMS_PER_PAGE = 45;
    private static final int PREVIOUS_SLOT = 45;
    private static final int INFO_SLOT = 49;
    private static final int NEXT_SLOT = 53;

    private StrainsMenuGUI() {
    }

    public static boolean isStrainsMenu(String title) {
        return title != null && title.startsWith(TITLE_PREFIX);
    }

    public static void open(Player player, int requestedPage) {
        List<StrainsPage> allPages = buildPages(StrainConfigLoader.getAllStrains());
        if (allPages.isEmpty()) {
            Inventory emptyGui = Bukkit.createInventory(null, GUI_SIZE, TITLE_PREFIX);
            emptyGui.setItem(INFO_SLOT, MenuUtils.createButtonItem("&cNo strains configured", Material.BARRIER));
            player.openInventory(emptyGui);
            return;
        }

        int page = Math.max(0, Math.min(requestedPage, allPages.size() - 1));
        StrainsPage current = allPages.get(page);
        String title = TITLE_PREFIX + ChatColor.GRAY + " - " + ChatColor.YELLOW + capitalize(current.rarity)
                + ChatColor.GRAY + " (Page " + current.rarityPage + "/" + current.rarityTotalPages + ")";
        Inventory gui = Bukkit.createInventory(null, GUI_SIZE, title);

        for (int i = 0; i < current.strains.size(); i++) {
            gui.setItem(i, createStrainItem(current.strains.get(i)));
        }

        if (page > 0) {
            gui.setItem(PREVIOUS_SLOT, MenuUtils.createButtonItem("&ePrevious Page", Material.ARROW));
        }
        gui.setItem(INFO_SLOT, MenuUtils.createButtonItem(
                "&a" + capitalize(current.rarity) + " &7Page " + current.rarityPage + "&8/&7" + current.rarityTotalPages,
                Material.BOOK
        ));
        if (page < allPages.size() - 1) {
            gui.setItem(NEXT_SLOT, MenuUtils.createButtonItem("&eNext Page", Material.ARROW));
        }

        player.openInventory(gui);
    }

    public static int parseGlobalPage(String title) {
        List<StrainsPage> allPages = buildPages(StrainConfigLoader.getAllStrains());
        String stripped = ChatColor.stripColor(title);

        for (int i = 0; i < allPages.size(); i++) {
            StrainsPage page = allPages.get(i);
            String expected = "Cannabis Strains - " + capitalize(page.rarity)
                    + " (Page " + page.rarityPage + "/" + page.rarityTotalPages + ")";
            if (expected.equals(stripped)) {
                return i;
            }
        }
        return 0;
    }

    private static List<StrainsPage> buildPages(Collection<StrainProfile> strains) {
        Map<String, List<StrainProfile>> grouped = new HashMap<>();
        for (StrainProfile strain : strains) {
            String rarity = normalizeRarity(strain.getRarity());
            grouped.computeIfAbsent(rarity, ignored -> new ArrayList<>()).add(strain);
        }

        List<StrainsPage> allPages = new ArrayList<>();
        for (String rarity : RARITY_ORDER) {
            List<StrainProfile> tierStrains = grouped.getOrDefault(rarity, new ArrayList<>());
            tierStrains.sort(Comparator.comparing(StrainProfile::getDisplayName));
            int totalPages = Math.max(1, (int) Math.ceil(tierStrains.size() / (double) ITEMS_PER_PAGE));

            for (int page = 0; page < totalPages; page++) {
                int start = page * ITEMS_PER_PAGE;
                int end = Math.min(start + ITEMS_PER_PAGE, tierStrains.size());
                List<StrainProfile> pageItems = new ArrayList<>();
                if (start < end) {
                    pageItems.addAll(tierStrains.subList(start, end));
                }
                allPages.add(new StrainsPage(rarity, page + 1, totalPages, pageItems));
            }
        }

        return allPages;
    }

    private static String normalizeRarity(String rarity) {
        if (rarity == null) return "common";
        String lowered = rarity.toLowerCase();
        if (RARITY_ORDER.contains(lowered)) {
            return lowered;
        }
        return "common";
    }

    private static ItemStack createStrainItem(StrainProfile strain) {
        ItemStack item = new ItemStack(Material.FERN);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

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
        return item;
    }

    private static String capitalize(String value) {
        if (value == null || value.isEmpty()) return "Unknown";
        return Character.toUpperCase(value.charAt(0)) + value.substring(1).toLowerCase();
    }

    private record StrainsPage(String rarity, int rarityPage, int rarityTotalPages, List<StrainProfile> strains) {
    }
}
