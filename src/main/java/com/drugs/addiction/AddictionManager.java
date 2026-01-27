package com.drugs.addiction;

import com.drugs.DrugRecipeHelper;
import com.drugs.DrugsV2;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class AddictionManager {

    private static final Map<UUID, Map<String, AddictionState>> addictions = new HashMap<>();
    private static AddictionConfig config;
    private static JavaPlugin plugin;
    private static BukkitTask heartbeatTask;

    private AddictionManager() {}

    public static void init(JavaPlugin pluginInstance) {
        plugin = pluginInstance;
        config = AddictionConfigLoader.load(pluginInstance);
        registerCureRecipes(pluginInstance);
        startHeartbeat(pluginInstance);
    }

    public static AddictionConfig getConfig() {
        return config;
    }

    public static Set<String> getEnabledCureIds() {
        Set<String> cureIds = new HashSet<>();
        if (config == null) return cureIds;
        for (Map.Entry<String, AddictionConfig.CureRule> entry : config.getCures().entrySet()) {
            AddictionConfig.CureRule rule = entry.getValue();
            if (rule != null && rule.enabled && rule.itemEnabled) {
                cureIds.add(entry.getKey());
            }
        }
        return cureIds;
    }

    public static ItemStack getCureItem(String cureId, int amount) {
        if (config == null || cureId == null) return null;
        AddictionConfig.CureRule cure = config.getCureRule(cureId);
        if (cure == null || !cure.enabled || !cure.itemEnabled) return null;
        int safeAmount = Math.max(1, amount);
        return buildCureItem(cure, safeAmount);
    }

    public static void purgePlayer(Player player) {
        if (player == null) return;
        addictions.remove(player.getUniqueId());
        if (config == null) return;
        for (String drugId : config.getDrugs().keySet()) {
            clearWithdrawalEffects(player, drugId);
            clearAddictedEffects(player, drugId);
        }
    }

    public static void reload(DrugsV2 pluginInstance) {
        if (pluginInstance == null) return;
        plugin = pluginInstance;
        config = AddictionConfigLoader.load(pluginInstance);
        registerCureRecipes(pluginInstance);
        startHeartbeat(pluginInstance);
    }

    public static void onDrugUse(Player player, String drugId) {
        if (config == null || !config.enabled || drugId == null) return;

        AddictionConfig.DrugRule rule = config.getDrugRule(drugId);
        if (rule == null || !rule.addictive) return;

        Map<String, AddictionState> playerAddictions =
                addictions.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());

        AddictionState state =
                playerAddictions.computeIfAbsent(drugId.toLowerCase(), k -> new AddictionState());

        state.addPoints(rule.pointsPerUse);
        state.updateLastDose();

        clearWithdrawalEffects(player, drugId);
    }

    public static Map<String, AddictionState> getAddictions(UUID uuid) {
        return addictions.getOrDefault(uuid, Map.of());
    }

    public static AddictionState getState(UUID uuid, String drugId) {
        Map<String, AddictionState> map = addictions.get(uuid);
        if (map == null) return null;
        return map.get(drugId.toLowerCase());
    }

    public static boolean isAddicted(UUID uuid, String drugId) {
        AddictionState state = getState(uuid, drugId);
        AddictionConfig.DrugRule rule = config.getDrugRule(drugId);

        if (state == null || rule == null) return false;
        return state.getPoints() >= rule.addictedAtPoints;
    }

    public static boolean applyCure(Player player, String cureId) {
        if (config == null || cureId == null) return false;

        AddictionConfig.CureRule cure = config.getCureRule(cureId);
        if (cure == null || !cure.enabled) return false;

        Map<String, AddictionState> playerAddictions = addictions.get(player.getUniqueId());
        if (playerAddictions == null) return false;

        boolean used = false;

        for (Map.Entry<String, AddictionState> entry : playerAddictions.entrySet()) {
            String drugId = entry.getKey();
            AddictionState state = entry.getValue();

            if (!cure.allowsDrug(drugId)) continue;
            AddictionConfig.DrugRule drugRule = config.getDrugRule(drugId);
            if (drugRule == null) continue;

            if (cure.clearsPoints) {
                state.setPoints(0);
            } else if (cure.reducePoints > 0) {
                state.removePoints(cure.reducePoints);
            }

            if (cure.blockWithdrawalSeconds > 0) {
                state.blockWithdrawalForSeconds(cure.blockWithdrawalSeconds);
            }

            state.updateLastDose();
            clearWithdrawalEffects(player, drugId);
            if (state.getPoints() < drugRule.addictedAtPoints) {
                clearAddictedEffects(player, drugId);
            }
            used = true;
        }

        return used;
    }

    public static String getCureIdFromItem(ItemStack item) {
        if (config == null || item == null) return null;

        for (Map.Entry<String, AddictionConfig.CureRule> entry : config.getCures().entrySet()) {
            AddictionConfig.CureRule cure = entry.getValue();
            if (!cure.enabled || !cure.itemEnabled) continue;
            if (item.getType() != cure.material) continue;

            if (cure.displayName != null && item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                if (meta == null || !meta.hasDisplayName()) continue;

                String rawName = ChatColor.stripColor(meta.getDisplayName());
                String cureName = ChatColor.stripColor(
                        ChatColor.translateAlternateColorCodes('&', cure.displayName));
                if (!rawName.equalsIgnoreCase(cureName)) continue;
            }

            return entry.getKey();
        }

        return null;
    }

    public static ItemStack buildCureItem(AddictionConfig.CureRule cure, int amount) {
        ItemStack item = new ItemStack(cure.material, amount);
        if (cure.displayName == null && cure.lore.isEmpty()) return item;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        if (cure.displayName != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', cure.displayName));
        }
        if (!cure.lore.isEmpty()) {
            meta.setLore(cure.lore.stream()
                    .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                    .toList());
        }
        item.setItemMeta(meta);
        return item;
    }

    public static void handleMilkConsumption(Player player) {
        if (config == null || config.milkCuresWithdrawal) return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Map<String, AddictionState> playerAddictions = addictions.get(player.getUniqueId());
            if (playerAddictions == null) return;

            for (Map.Entry<String, AddictionState> entry : playerAddictions.entrySet()) {
                String drugId = entry.getKey();
                AddictionState state = entry.getValue();
                AddictionConfig.DrugRule rule = config.getDrugRule(drugId);
                if (rule == null || !rule.addictive) continue;
                if (!isAddicted(player.getUniqueId(), drugId)) continue;
                if (state.isWithdrawalBlocked()) continue;

                long secondsSinceDose = (System.currentTimeMillis() - state.getLastDoseMillis()) / 1000L;
                if (secondsSinceDose >= rule.withdrawalAfterSeconds) {
                    applyWithdrawalEffects(player, rule);
                }
            }
        }, 1L);
    }

    static void clearWithdrawalEffects(Player player, String drugId) {
        AddictionConfig.DrugRule rule = config.getDrugRule(drugId);
        if (rule == null) return;
        clearEffects(player, rule.withdrawalEffects, true);
    }

    static void clearAddictedEffects(Player player, String drugId) {
        AddictionConfig.DrugRule rule = config.getDrugRule(drugId);
        if (rule == null) return;
        clearEffects(player, rule.addictedEffects, true);
    }

    static void applyWithdrawalEffects(Player player, AddictionConfig.DrugRule rule) {
        applyEffects(player, rule.withdrawalEffects);
    }

    static void applyAddictedEffects(Player player, AddictionConfig.DrugRule rule) {
        applyEffects(player, rule.addictedEffects);
    }

    private static void applyEffects(Player player, Iterable<AddictionConfig.EffectSpec> effects) {
        if (effects == null) return;
        for (AddictionConfig.EffectSpec effect : effects) {
            if (effect.type == null) continue;
            player.addPotionEffect(new PotionEffect(
                    effect.type,
                    config.infiniteDurationTicks,
                    Math.max(0, effect.amplifier),
                    true,
                    false,
                    false
            ));
        }
    }

    private static void clearEffects(
            Player player,
            Iterable<AddictionConfig.EffectSpec> effects,
            boolean onlyInfinite
    ) {
        if (effects == null) return;
        for (AddictionConfig.EffectSpec effect : effects) {
            if (effect.type == null) continue;
            if (!onlyInfinite) {
                player.removePotionEffect(effect.type);
                continue;
            }

            PotionEffect active = player.getPotionEffect(effect.type);
            if (active == null) continue;
            if (config == null) continue;
            if (active.getDuration() >= config.infiniteDurationTicks - 1) {
                player.removePotionEffect(effect.type);
            }
        }
    }

    private static void startHeartbeat(JavaPlugin pluginInstance) {
        if (config == null || !config.enabled) return;
        long interval = Math.max(1, config.heartbeatTicks);
        if (heartbeatTask != null) {
            heartbeatTask.cancel();
        }
        heartbeatTask = Bukkit.getScheduler().runTaskTimer(
                pluginInstance,
                new AddictionTickTask(),
                interval,
                interval
        );
    }

    static void runHeartbeat() {
        if (config == null || !config.enabled) return;

        double intervalSeconds = config.heartbeatTicks / 20.0;

        for (Player player : Bukkit.getOnlinePlayers()) {
            Map<String, AddictionState> map = addictions.get(player.getUniqueId());
            if (map == null || map.isEmpty()) continue;

            for (Map.Entry<String, AddictionState> entry : map.entrySet()) {
                String drugId = entry.getKey();
                AddictionState state = entry.getValue();
                AddictionConfig.DrugRule rule = config.getDrugRule(drugId);
                if (rule == null || !rule.addictive) continue;

                if (rule.decayEnabled && rule.decayPointsPerMinute > 0 && state.getPoints() > 0) {
                    double decay = rule.decayPointsPerMinute * (intervalSeconds / 60.0);
                    state.removePoints(decay);
                }

                boolean addicted = state.getPoints() >= rule.addictedAtPoints;
                if (!addicted) {
                    clearWithdrawalEffects(player, drugId);
                    clearAddictedEffects(player, drugId);
                    continue;
                }

                applyAddictedEffects(player, rule);

                if (state.isWithdrawalBlocked()) {
                    clearWithdrawalEffects(player, drugId);
                    continue;
                }

                long secondsSinceDose = (System.currentTimeMillis() - state.getLastDoseMillis()) / 1000L;
                if (secondsSinceDose >= rule.withdrawalAfterSeconds) {
                    applyWithdrawalEffects(player, rule);
                } else {
                    clearWithdrawalEffects(player, drugId);
                }
            }
        }
    }

    private static void registerCureRecipes(JavaPlugin pluginInstance) {
        if (!(pluginInstance instanceof DrugsV2 drugsPlugin)) return;
        if (drugsPlugin.getRecipesConfig() == null) return;

        for (Map.Entry<String, AddictionConfig.CureRule> entry : config.getCures().entrySet()) {
            String cureId = entry.getKey();
            AddictionConfig.CureRule cure = entry.getValue();
            if (!cure.enabled || !cure.itemEnabled) continue;

            ConfigurationSection recipeSection = drugsPlugin.getRecipesConfig().getConfigurationSection(cureId);
            if (recipeSection == null) continue;

            ItemStack result = buildCureItem(cure, 1);
            DrugRecipeHelper.registerCustomRecipe(cureId, recipeSection, result, pluginInstance);
        }
    }
}
