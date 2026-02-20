package com.drugs;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

public class BongListener implements Listener {

    private final Map<String, Long> cooldowns = new HashMap<>();

    @EventHandler
    public void onPlace(PlayerInteractEvent event) {
        if (!BongConfigLoader.isEnabled()) return;
        if (event.getHand() != EquipmentSlot.HAND || !event.getAction().isRightClick()) return;

        ItemStack handItem = event.getPlayer().getInventory().getItemInMainHand();
        if (!BongItemFactory.isBongItem(handItem)) return;

        Block clicked = event.getClickedBlock();
        if (clicked == null || !clicked.getType().isSolid()) return;

        Block target = clicked.getRelative(event.getBlockFace());
        if (!target.getType().isAir()) {
            event.getPlayer().sendMessage("§cYou need an empty block to place a bong.");
            return;
        }

        Location anchor = target.getLocation();
        if (BongRegistry.get(anchor) != null) {
            event.getPlayer().sendMessage("§cThere is already a bong here.");
            return;
        }

        spawnOrReplace(anchor, event.getPlayer().getLocation().getYaw());
        event.setCancelled(true);

        if (event.getPlayer().getGameMode() != GameMode.CREATIVE) {
            handItem.setAmount(handItem.getAmount() - 1);
        }

        event.getPlayer().playSound(anchor, Sound.BLOCK_GLASS_PLACE, 1.0f, 1.0f);
    }

    @EventHandler
    public void onUse(PlayerInteractEntityEvent event) {
        if (!BongConfigLoader.isEnabled()) return;

        if (!(event.getRightClicked() instanceof Interaction interaction)) return;
        BongRegistry.BongData data = BongRegistry.findByInteraction(interaction.getUniqueId());
        if (data == null) return;

        event.setCancelled(true);
        Player player = event.getPlayer();

        if (player.isSneaking() && player.getInventory().getItemInMainHand().getType() == Material.AIR) {
            Location anchor = data.getAnchor();
            if (anchor != null) {
                BongRegistry.remove(anchor);
                anchor.getWorld().dropItemNaturally(anchor.clone().add(0.5, 0.2, 0.5), BongItemFactory.createBongItem(1));
            }
            player.sendMessage("§7You packed up the bong.");
            return;
        }

        if (isOnCooldown(player)) {
            player.sendMessage("§7The bong is still warm. Give it a second.");
            return;
        }

        ItemStack weed = resolveWeedItem(player);
        if (weed == null) {
            player.sendMessage("§cHold strain-tagged weed to use the bong.");
            return;
        }

        String strainId = DrugItemMetadata.getStrainId(weed);
        if (strainId == null) {
            strainId = DrugItemMetadata.DEFAULT_STRAIN_ID;
        }

        String baseDrug = BongConfigLoader.getBaseDrugId();
        DrugEffectProfile profile = DrugRegistry.getProfileById(baseDrug);
        if (profile == null) {
            player.sendMessage("§cBong config error: invalid base-drug-id '" + baseDrug + "'.");
            return;
        }

        ItemStack source = DrugRegistry.getDrugItem(baseDrug, 1, strainId);
        if (source == null) {
            source = DrugRegistry.getDrugItem(baseDrug, 1);
        }

        profile.applyEffects(player, source);
        if (player.getGameMode() != GameMode.CREATIVE) {
            weed.setAmount(weed.getAmount() - 1);
        }

        cooldowns.put(player.getUniqueId().toString(), System.currentTimeMillis());
        player.playSound(player.getLocation(), Sound.BLOCK_BUBBLE_COLUMN_BUBBLE_POP, 1.0f, 1.25f);
        StrainProfile strain = StrainConfigLoader.getStrain(strainId);
        String strainName = strain != null ? strain.getDisplayName() : strainId;
        player.sendMessage("§2You take a bong hit. §7(Strain: §a" + strainName + "§7)");
    }

    public void spawnOrReplace(Location anchor, float yaw) {
        BongRegistry.remove(anchor);

        Location displayLocation = anchor.clone().add(0.5, 0.15, 0.5);
        ItemDisplay display = anchor.getWorld().spawn(displayLocation, ItemDisplay.class, spawned -> {
            spawned.setPersistent(true);
            spawned.setItemStack(BongItemFactory.createBongItem(1));
            spawned.setRotation(yaw, 0.0f);
            spawned.setTransformation(new Transformation(
                    new Vector3f(0f, 0f, 0f),
                    new Quaternionf(),
                    new Vector3f(1.0f, 1.0f, 1.0f),
                    new Quaternionf()
            ));
        });

        Location interactionLocation = anchor.clone().add(0.5, 0.35, 0.5);
        Interaction hitbox = anchor.getWorld().spawn(interactionLocation, Interaction.class, spawned -> {
            spawned.setInteractionWidth(0.6f);
            spawned.setInteractionHeight(0.8f);
            spawned.setPersistent(true);
        });

        BongRegistry.put(anchor, new BongRegistry.BongData(anchor, yaw, display.getUniqueId(), hitbox.getUniqueId()));
    }

    private boolean isOnCooldown(Player player) {
        Long lastUse = cooldowns.get(player.getUniqueId().toString());
        return lastUse != null && (System.currentTimeMillis() - lastUse) < BongConfigLoader.getCooldownMillis();
    }

    private ItemStack resolveWeedItem(Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        if (isConsumableWeed(main)) return main;

        ItemStack off = player.getInventory().getItemInOffHand();
        if (isConsumableWeed(off)) return off;

        return null;
    }

    private boolean isConsumableWeed(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        String itemType = DrugItemMetadata.getItemType(meta);
        return itemType != null && itemType.equalsIgnoreCase("cannabis_plant");
    }
}
