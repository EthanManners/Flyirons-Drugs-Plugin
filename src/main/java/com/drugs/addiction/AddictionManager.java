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
import java.util.Locale;
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
            if (rule != null && rule.enabled) {
                cureIds.add(entry.getKey());
            }
        }
        return cureIds;
    }

    public static ItemStack getCureItem(String cureId, int amount) {
        if (config == null || cureId == null) return null;
        AddictionConfig.CureRule cure = config.getCureRule(cureId);
        if (cure == null || !cure.enabled) return null;
        int safeAmount = Math.max(1, amount);
        return buildCureItem(cure, safeAmount);
    }

    public static void purgePlayer(Player player) {
        if (player == null) return;
        addictions.remove(player.getUniqueId());
        if (config == null) return;
        for (AddictionConfig.DrugRule rule : config.getDrugs().values()) {
            clearWithdrawalEffects(player, rule);
            clearAddictedEffects(player, rule);
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
        if (!isEnabled() || player == null) return;

        AddictionConfig.DrugRule rule = getDrugRule(drugId);
        if (rule == null || !rule.addictive) return;

        AddictionState state = getOrCreateState(player.getUniqueId(), drugId);
        if (state == null) return;

        state.addPoints(rule.pointsPerUse);
        state.updateLastDose();
        clearWithdrawalEffects(player, rule);
    }

    public static Map<String, AddictionState> getAddictions(UUID uuid) {
        return addictions.getOrDefault(uuid, Map.of());
    }

    public static AddictionState getState(UUID uuid, String drugId) {
        String normalizedId = normalizeId(drugId);
        if (normalizedId == null) return null;
        Map<String, AddictionState> map = addictions.get(uuid);
        if (map == null) return null;
        return map.get(normalizedId);
    }

    public static boolean isAddicted(UUID uuid, String drugId) {
        AddictionState state = getState(uuid, drugId);
        AddictionConfig.DrugRule rule = getDrugRule(drugId);

        if (state == null || rule == null) return false;
        return state.getPoints() >= rule.addictedAtPoints;
    }

    public static boolean applyCure(Player player, String cureId) {
        if (!isEnabled() || player == null) return false;

        AddictionConfig.CureRule cure = config.getCureRule(cureId);
        if (cure == null || !cure.enabled) return false;

        Map<String, AddictionState> playerAddictions = addictions.get(player.getUniqueId());
        if (playerAddictions == null) return false;

        boolean used = false;

        for (Map.Entry<String, AddictionState> entry : playerAddictions.entrySet()) {
            String drugId = entry.getKey();
            AddictionState state = entry.getValue();
            AddictionConfig.DrugRule rule = getDrugRule(drugId);
            if (rule == null || !rule.addictive) continue;

            if (!cure.allowsDrug(drugId)) continue;

            if (cure.clearsPoints) {
                state.setPoints(0);
            } else if (cure.reducePoints > 0) {
                state.removePoints(cure.reducePoints);
            }

            if (cure.blockWithdrawalSeconds > 0) {
                state.blockWithdrawalForSeconds(cure.blockWithdrawalSeconds);
            }

            clearWithdrawalEffects(player, rule);
            clearAddictedEffects(player, rule);
            used = true;
        }

        return used;
    }

    public static String getCureIdFromItem(ItemStack item) {
        if (config == null || item == null) return null;

        for (Map.Entry<String, AddictionConfig.CureRule> entry : config.getCures().entrySet()) {
            AddictionConfig.CureRule cure = entry.getValue();
            if (!cure.enabled) continue;
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
        if (config == null || config.milkCuresWithdrawal || player == null) return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Map<String, AddictionState> playerAddictions = addictions.get(player.getUniqueId());
            if (playerAddictions == null) return;

            long nowMillis = System.currentTimeMillis();
            for (Map.Entry<String, AddictionState> entry : playerAddictions.entrySet()) {
                AddictionConfig.DrugRule rule = getDrugRule(entry.getKey());
                if (rule == null || !rule.addictive) continue;

                AddictionState state = entry.getValue();
                if (state == null || state.isWithdrawalBlocked()) continue;
                if (state.getPoints() < rule.addictedAtPoints) continue;

                long secondsSinceDose = (nowMillis - state.getLastDoseMillis()) / 1000L;
                if (secondsSinceDose >= rule.withdrawalAfterSeconds) {
                    applyWithdrawalEffects(player, rule);
                }
            }
        }, 1L);
    }

    static void clearWithdrawalEffects(Player player, String drugId) {
        AddictionConfig.DrugRule rule = getDrugRule(drugId);
        if (rule == null) return;
        clearWithdrawalEffects(player, rule);
    }

    static void clearAddictedEffects(Player player, String drugId) {
        AddictionConfig.DrugRule rule = getDrugRule(drugId);
        if (rule == null) return;
        clearAddictedEffects(player, rule);
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
        if (heartbeatTask != null) {
            heartbeatTask.cancel();
        }
        if (config == null || !config.enabled) return;
        long interval = Math.max(1, config.heartbeatTicks);
        heartbeatTask = Bukkit.getScheduler().runTaskTimer(
                pluginInstance,
                new AddictionTickTask(),
                interval,
                interval
        );
    }

    static void runHeartbeat() {
        if (!isEnabled()) return;

        double intervalSeconds = config.heartbeatTicks / 20.0;
        long nowMillis = System.currentTimeMillis();

        for (Player player : Bukkit.getOnlinePlayers()) {
            Map<String, AddictionState> map = addictions.get(player.getUniqueId());
            if (map == null || map.isEmpty()) continue;

            for (Map.Entry<String, AddictionState> entry : map.entrySet()) {
                String drugId = entry.getKey();
                AddictionState state = entry.getValue();
                AddictionConfig.DrugRule rule = getDrugRule(drugId);
                if (rule == null || !rule.addictive || state == null) continue;

                if (rule.decayEnabled && rule.decayPointsPerMinute > 0 && state.getPoints() > 0) {
                    double decay = rule.decayPointsPerMinute * (intervalSeconds / 60.0);
                    state.removePoints(decay);
                }

                if (state.getPoints() < rule.addictedAtPoints) {
                    clearWithdrawalEffects(player, rule);
                    clearAddictedEffects(player, rule);
                    continue;
                }

                applyAddictedEffects(player, rule);

                if (state.isWithdrawalBlocked()) {
                    clearWithdrawalEffects(player, rule);
                    continue;
                }

                long secondsSinceDose = (nowMillis - state.getLastDoseMillis()) / 1000L;
                if (secondsSinceDose >= rule.withdrawalAfterSeconds) {
                    applyWithdrawalEffects(player, rule);
                } else {
                    clearWithdrawalEffects(player, rule);
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
            if (!cure.enabled) continue;

            ConfigurationSection recipeSection = drugsPlugin.getRecipesConfig().getConfigurationSection(cureId);
            if (recipeSection == null) continue;

            ItemStack result = buildCureItem(cure, 1);
            DrugRecipeHelper.registerCustomRecipe(cureId, recipeSection, result, pluginInstance);
        }
    }

    private static AddictionConfig.DrugRule getDrugRule(String drugId) {
        if (config == null) return null;
        return config.getDrugRule(drugId);
    }

    private static String normalizeId(String id) {
        if (id == null) return null;
        return id.toLowerCase(Locale.ROOT);
    }

    private static AddictionState getOrCreateState(UUID uuid, String drugId) {
        if (uuid == null) return null;
        String normalizedId = normalizeId(drugId);
        if (normalizedId == null) return null;
        Map<String, AddictionState> playerAddictions =
                addictions.computeIfAbsent(uuid, k -> new HashMap<>());
        return playerAddictions.computeIfAbsent(normalizedId, k -> new AddictionState());
    }

    private static boolean isEnabled() {
        return config != null && config.enabled;
    }

    private static void clearWithdrawalEffects(Player player, AddictionConfig.DrugRule rule) {
        if (rule == null) return;
        clearEffects(player, rule.withdrawalEffects, true);
    }

    private static void clearAddictedEffects(Player player, AddictionConfig.DrugRule rule) {
        if (rule == null) return;
        clearEffects(player, rule.addictedEffects, true);
    }
}
