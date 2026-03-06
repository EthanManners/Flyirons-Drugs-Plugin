package com.drugs.weedfarm;

import com.drugs.DrugItemMetadata;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
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

        manager.removeFarm(farm);
        event.setDropItems(false);
        event.getPlayer().sendMessage(ChatColor.RED + "Farm controller removed. Workers unassigned.");
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
        WeedFarm farm = manager.getByFarmId(farmId);
        if (farm == null || event.getCurrentItem() == null) {
            return;
        }

        switch (event.getCurrentItem().getType()) {
            case STICK -> {
                player.getInventory().addItem(WeedFarmItems.createAreaWand(farm.getFarmId()));
                player.sendMessage(ChatColor.AQUA + "Use the stick wand to select two corners (max 16x16). ");
            }
            case LEVER -> {
                farm.setEnabled(!farm.isEnabled());
                player.sendMessage(farm.isEnabled()
                        ? ChatColor.GREEN + "Farm started."
                        : ChatColor.RED + "Farm stopped.");
                openControllerGui(player, farm);
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
        if (areaFarmId == null || event.getClickedBlock() == null) {
            return;
        }

        event.setCancelled(true);
        WeedFarm farm = manager.getByFarmId(areaFarmId);
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
                if (!farm.canApplyRegion(first, second)) {
                    event.getPlayer().sendMessage(ChatColor.RED + "Farm area too large. Max size is 16x16 blocks.");
                    return;
                }
                farm.setRegion(first.getWorld(), first, second);
                consumeAreaWand(event.getPlayer(), areaFarmId);
                event.getPlayer().sendMessage(ChatColor.GREEN + "Farm area updated.");
            }
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

        WeedFarm farm = manager.getByFarmId(farmId);
        if (farm == null) {
            return;
        }

        RayTraceResult trace = event.getPlayer().rayTraceBlocks(6.0);
        if (trace == null || trace.getHitBlock() == null || manager.getByController(trace.getHitBlock().getLocation()) != farm) {
            event.getPlayer().sendMessage(ChatColor.RED + "Look at the farm controller while binding workers.");
            return;
        }

        if (!manager.assignVillager(farm, villager.getUniqueId())) {
            event.getPlayer().sendMessage(ChatColor.RED + "This farm already has the max of 5 villagers.");
            return;
        }

        decrementHeld(event.getPlayer());
        event.getPlayer().sendMessage(ChatColor.LIGHT_PURPLE + "Villager assigned to farm " + farm.getFarmId() + ".");
        event.setCancelled(true);
    }

    private void consumeAreaWand(Player player, String farmId) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (farmId.equals(WeedFarmItems.parseFarmId(stack, WeedFarmItems.ITEM_TYPE_AREA_WAND))) {
                player.getInventory().setItem(i, null);
                return;
            }
        }
    }

    private void openControllerGui(Player player, WeedFarm farm) {
        Inventory inventory = Bukkit.createInventory(null, 27, GUI_TITLE_PREFIX + farm.getFarmId());
        inventory.setItem(10, named(Material.STICK, ChatColor.AQUA + "Define Farm Area"));
        inventory.setItem(12, named(Material.LEVER, (farm.isEnabled() ? ChatColor.RED + "Stop Farm" : ChatColor.GREEN + "Start Farm")));
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
        int x = farm.hasRegion() ? (farm.getMaxX() - farm.getMinX() + 1) : 0;
        int z = farm.hasRegion() ? (farm.getMaxZ() - farm.getMinZ() + 1) : 0;
        return "Farm " + farm.getFarmId() + " | "
                + (farm.isEnabled() ? "Enabled" : "Disabled")
                + " | Villagers: " + farm.getAssignedVillagers().size() + "/" + WeedFarm.MAX_WORKERS
                + " | Area: " + x + "x" + z;
    }
}
