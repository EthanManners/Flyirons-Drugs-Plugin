package com.drugs;

import org.bukkit.Bukkit;
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
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BongListener implements Listener {

    private static final String WATER_IN_GLASS_TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjA0YzgyMTRhYjhkZDAwNGJlYmE2YTQxODg2MDQ0NzBhODM4ZmViMWFlOTJlZTYyZDFkMTVlMGRmMGNlYmZmNyJ9fX0=";

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
        if (!target.getType().isAir() || !target.getRelative(0, 1, 0).getType().isAir()) {
            event.getPlayer().sendMessage("§cYou need a clear 1x1x2 space to place a bong.");
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

    @EventHandler
    public void onPunch(EntityDamageByEntityEvent event) {
        if (!BongConfigLoader.isEnabled()) return;
        if (!(event.getDamager() instanceof Player player)) return;

        BongRegistry.BongData data = BongRegistry.findByEntity(event.getEntity().getUniqueId());
        if (data == null) return;

        event.setCancelled(true);
        Location anchor = data.getAnchor();
        if (anchor == null) return;

        BongRegistry.remove(anchor);
        if (player.getGameMode() != GameMode.CREATIVE) {
            anchor.getWorld().dropItemNaturally(anchor.clone().add(0.5, 0.2, 0.5), BongItemFactory.createBongItem(1));
        }

        player.playSound(anchor, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.9f);
    }

    public void spawnOrReplace(Location anchor, float yaw) {
        BongRegistry.remove(anchor);

        List<UUID> displayIds = new ArrayList<>();
        displayIds.add(spawnDisplay(anchor, yaw, createWaterHead(),
                new Vector3f(0.5f, 0.36f, 0.5f),
                new Vector3f(0.78f, 0.78f, 0.78f),
                new Quaternionf()));

        displayIds.add(spawnDisplay(anchor, yaw, new ItemStack(Material.GLASS),
                new Vector3f(0.5f, 0.62f, 0.5f),
                new Vector3f(0.24f, 0.24f, 0.24f),
                new Quaternionf()));
        displayIds.add(spawnDisplay(anchor, yaw, new ItemStack(Material.GLASS),
                new Vector3f(0.5f, 0.86f, 0.5f),
                new Vector3f(0.24f, 0.24f, 0.24f),
                new Quaternionf()));

        displayIds.add(spawnDisplay(anchor, yaw, new ItemStack(Material.GOLDEN_SHOVEL),
                new Vector3f(0.74f, 0.38f, 0.58f),
                new Vector3f(0.20f, 0.20f, 0.20f),
                new Quaternionf()
                        .rotateY((float) Math.toRadians(82))
                        .rotateZ((float) Math.toRadians(-38))));

        Location interactionLocation = anchor.clone().add(0.5, 0.64, 0.5);
        Interaction hitbox = anchor.getWorld().spawn(interactionLocation, Interaction.class, spawned -> {
            spawned.setInteractionWidth(0.8f);
            spawned.setInteractionHeight(1.15f);
            spawned.setPersistent(true);
        });

        BongRegistry.put(anchor, new BongRegistry.BongData(anchor, yaw, displayIds, hitbox.getUniqueId()));
    }

    private UUID spawnDisplay(Location anchor, float yaw, ItemStack displayItem, Vector3f offset, Vector3f scale, Quaternionf rightRotation) {
        Location displayLocation = anchor.clone().add(offset.x, offset.y, offset.z);
        ItemDisplay display = anchor.getWorld().spawn(displayLocation, ItemDisplay.class, spawned -> {
            spawned.setPersistent(true);
            spawned.setItemStack(displayItem);
            spawned.setRotation(yaw, 0.0f);
            spawned.setTransformation(new Transformation(
                    new Vector3f(0f, 0f, 0f),
                    new Quaternionf(),
                    scale,
                    rightRotation
            ));
        });
        return display.getUniqueId();
    }

    private ItemStack createWaterHead() {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta == null) {
            return head;
        }

        meta.setDisplayName("§6§n§lWater in Glass");
        meta.setLore(List.of("§7Custom Head ID: 118716", "§9www.minecraft-heads.com"));
        applyHeadTexture(meta);
        head.setItemMeta(meta);
        return head;
    }

    private void applyHeadTexture(SkullMeta meta) {
        try {
            PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
            PlayerTextures textures = profile.getTextures();
            textures.setSkin(URI.create(extractTextureUrl(WATER_IN_GLASS_TEXTURE)).toURL());
            profile.setTextures(textures);
            meta.setOwnerProfile(profile);
        } catch (Exception ex) {
            Bukkit.getLogger().warning("[DrugsV2] Failed to apply bong head texture: " + ex.getMessage());
        }
    }

    private String extractTextureUrl(String base64Texture) {
        byte[] decoded = java.util.Base64.getDecoder().decode(base64Texture);
        String json = new String(decoded, java.nio.charset.StandardCharsets.UTF_8);
        String marker = "\"url\":\"";
        int start = json.indexOf(marker);
        if (start < 0) {
            return "http://textures.minecraft.net/texture/f04c8214ab8dd004beba6a4188604470a838feb1ae92ee62d1d15e0df0cebff7";
        }
        start += marker.length();
        int end = json.indexOf('"', start);
        if (end < 0) {
            return "http://textures.minecraft.net/texture/f04c8214ab8dd004beba6a4188604470a838feb1ae92ee62d1d15e0df0cebff7";
        }
        return json.substring(start, end);
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
