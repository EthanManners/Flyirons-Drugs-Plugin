package com.drugs.weedfarm;

import com.drugs.DrugItemMetadata;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.RayTraceResult;

import java.util.HashMap;
import java.util.Map;

public class WeedFarmControllerListener implements Listener {
    private static final String GUI_TITLE_PREFIX = ChatColor.DARK_GREEN + "Farm Controller ";

    private final WeedFarmManager manager;
    private final Map<Player, Location> pos1 = new HashMap<>();
    private final Map<Player, Location> pos2 = new HashMap<>();

    public WeedFarmControllerListener(WeedFarmManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onControllerPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (item == null || !item.hasItemMeta()) {
            return;
        }
        String type = DrugItemMetadata.getItemType(item.getItemMeta());
        if (!WeedFarmItems.ITEM_TYPE_CONTROLLER.equalsIgnoreCase(type)) {
            return;
        }

        WeedFarm farm = manager.createFarm(event.getBlockPlaced().getLocation());
        event.getPlayer().sendMessage(ChatColor.GREEN + "Farm controller created (ID " + farm.getFarmId() + ").");
    }

    @EventHandler
    public void onControllerBreak(BlockBreakEvent event) {
        WeedFarm farm = manager.getByController(event.getBlock().getLocation());
        if (farm == null) {
            return;
        }
        event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), WeedFarmItems.createControllerItem());
    }

    @EventHandler
    public void onInteractController(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null || event.getClickedBlock().getType() != Material.BARREL) {
            return;
        }

        WeedFarm farm = manager.getByController(event.getClickedBlock().getLocation());
        if (farm == null) {
            return;
        }

        event.setCancelled(true);
        openControllerGui(event.getPlayer(), farm);
    }

    @EventHandler
    public void onGuiClick(InventoryClickEvent event) {
        if (event.getView().getTitle() == null || !event.getView().getTitle().startsWith(GUI_TITLE_PREFIX)) {
            return;
        }
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        String farmId = ChatColor.stripColor(event.getView().getTitle().replace("Farm Controller ", "")).trim();
        WeedFarm farm = manager.getFarms().stream().filter(f -> f.getFarmId().equalsIgnoreCase(farmId)).findFirst().orElse(null);
        if (farm == null || event.getCurrentItem() == null) {
            return;
        }

        switch (event.getCurrentItem().getType()) {
            case GOLDEN_HOE -> {
                player.getInventory().addItem(WeedFarmItems.createAreaWand(farm.getFarmId()));
                player.sendMessage(ChatColor.AQUA + "Use the wand to select two corners for the farm.");
            }
            case CHEST -> {
                player.getInventory().addItem(WeedFarmItems.createChestWand(farm.getFarmId()));
                player.sendMessage(ChatColor.GOLD + "Right-click a chest to link this farm.");
            }
            case PAPER -> {
                if (!consumeEmerald(player)) {
                    player.sendMessage(ChatColor.RED + "You need 1 emerald for a work contract.");
                    return;
                }
                player.getInventory().addItem(WeedFarmItems.createContract(farm.getFarmId()));
                player.sendMessage(ChatColor.LIGHT_PURPLE + "Work contract created.");
            }
            case BOOK -> player.sendMessage(ChatColor.GREEN + farmInfo(farm));
            default -> {
            }
        }
    }

    @EventHandler
    public void onWandInteract(PlayerInteractEvent event) {
        ItemStack inHand = event.getItem();
        if (inHand == null || !inHand.hasItemMeta()) {
            return;
        }
        String areaFarmId = WeedFarmItems.parseFarmId(inHand, WeedFarmItems.ITEM_TYPE_AREA_WAND);
        if (areaFarmId != null && event.getClickedBlock() != null) {
            event.setCancelled(true);
            WeedFarm farm = manager.getFarms().stream().filter(f -> f.getFarmId().equals(areaFarmId)).findFirst().orElse(null);
            if (farm == null) return;

            if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                pos1.put(event.getPlayer(), event.getClickedBlock().getLocation());
                event.getPlayer().sendActionBar(ChatColor.AQUA + "Pos1: " + format(event.getClickedBlock().getLocation()));
            } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                pos2.put(event.getPlayer(), event.getClickedBlock().getLocation());
                event.getPlayer().sendActionBar(ChatColor.AQUA + "Pos2: " + format(event.getClickedBlock().getLocation()));
            }

            if (pos1.containsKey(event.getPlayer()) && pos2.containsKey(event.getPlayer())) {
                Location first = pos1.remove(event.getPlayer());
                Location second = pos2.remove(event.getPlayer());
                if (first != null && second != null && first.getWorld() != null && first.getWorld().equals(second.getWorld())) {
                    farm.setRegion(first.getWorld(), first, second);
                    event.getPlayer().sendMessage(ChatColor.GREEN + "Farm area updated.");
                }
            }
            return;
        }

        String chestFarmId = WeedFarmItems.parseFarmId(inHand, WeedFarmItems.ITEM_TYPE_CHEST_WAND);
        if (chestFarmId != null && event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            if (!(event.getClickedBlock().getState() instanceof Chest)) {
                return;
            }
            WeedFarm farm = manager.getFarms().stream().filter(f -> f.getFarmId().equals(chestFarmId)).findFirst().orElse(null);
            if (farm == null) return;
            farm.setChestLocation(event.getClickedBlock().getLocation());
            event.getPlayer().sendMessage(ChatColor.GOLD + "Farm chest linked.");
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onVillagerContract(PlayerInteractAtEntityEvent event) {
        Entity clicked = event.getRightClicked();
        if (!(clicked instanceof Villager villager)) {
            return;
        }

        ItemStack inHand = event.getPlayer().getInventory().getItemInMainHand();
        String farmId = WeedFarmItems.parseFarmId(inHand, WeedFarmItems.ITEM_TYPE_CONTRACT);
        if (farmId == null) {
            return;
        }

        WeedFarm farm = manager.getFarms().stream().filter(f -> f.getFarmId().equals(farmId)).findFirst().orElse(null);
        if (farm == null) {
            return;
        }

        RayTraceResult trace = event.getPlayer().rayTraceBlocks(6.0);
        if (trace == null || trace.getHitBlock() == null || manager.getByController(trace.getHitBlock().getLocation()) != farm) {
            event.getPlayer().sendMessage(ChatColor.RED + "Look at the farm controller while binding workers.");
            return;
        }

        manager.assignVillager(farm, villager.getUniqueId());
        decrementHeld(event.getPlayer());
        event.getPlayer().sendMessage(ChatColor.LIGHT_PURPLE + "Villager assigned to farm " + farm.getFarmId() + ".");
        event.setCancelled(true);
    }

    private void openControllerGui(Player player, WeedFarm farm) {
        Inventory inventory = Bukkit.createInventory(null, 27, GUI_TITLE_PREFIX + farm.getFarmId());
        inventory.setItem(10, named(Material.GOLDEN_HOE, ChatColor.AQUA + "Define Farm Area"));
        inventory.setItem(12, named(Material.CHEST, ChatColor.GOLD + "Link Farm Chest"));
        inventory.setItem(14, named(Material.PAPER, ChatColor.LIGHT_PURPLE + "Get Work Contract"));
        inventory.setItem(16, named(Material.BOOK, ChatColor.GREEN + "Show Farm Info"));
        player.openInventory(inventory);
    }

    private ItemStack named(Material type, String displayName) {
        ItemStack stack = new ItemStack(type);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private String format(Location location) {
        return location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }

    private boolean consumeEmerald(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack stack = contents[i];
            if (stack != null && stack.getType() == Material.EMERALD && stack.getAmount() > 0) {
                stack.setAmount(stack.getAmount() - 1);
                if (stack.getAmount() <= 0) {
                    player.getInventory().setItem(i, null);
                }
                return true;
            }
        }
        return false;
    }

    private void decrementHeld(Player player) {
        ItemStack held = player.getInventory().getItemInMainHand();
        held.setAmount(held.getAmount() - 1);
        if (held.getAmount() <= 0) {
            player.getInventory().setItemInMainHand(null);
        }
    }

    private String farmInfo(WeedFarm farm) {
        return "Farm " + farm.getFarmId() + " | Villagers: " + farm.getAssignedVillagers().size() + " | Chest: " + (farm.getChestLocation() == null ? "none" : format(farm.getChestLocation()));
    }
}
