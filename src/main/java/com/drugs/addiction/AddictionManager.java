package com.drugs.addiction;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class AddictionManager {

    /**
     * UUID -> (drugId -> AddictionState)
     */
    private static final Map<UUID, Map<String, AddictionState>> addictions = new HashMap<>();

    private static AddictionConfig config;

    private AddictionManager() {}

    /* ======================================================
     * Initialization
     * ====================================================== */

    public static void init(JavaPlugin plugin) {
        config = AddictionConfigLoader.load(plugin);
    }

    public static AddictionConfig getConfig() {
        return config;
    }

    /* ======================================================
     * Drug use entry point (called from DrugUseListener)
     * ====================================================== */

    public static void onDrugUse(Player player, String drugId) {
        if (config == null || !config.enabled) return;

        drugId = drugId.toLowerCase();

        AddictionConfig.DrugAddictionRule rule = config.drugs.get(drugId);
        if (rule == null) return; // drug not addictive

        UUID uuid = player.getUniqueId();

        Map<String, AddictionState> playerAddictions =
                addictions.computeIfAbsent(uuid, k -> new HashMap<>());

        AddictionState state =
                playerAddictions.computeIfAbsent(drugId, k -> new AddictionState());

        // ----------------------------------
        // Apply addiction points
        // ----------------------------------
        state.addPoints(rule.pointsPerUse);
        state.updateLastDose();

        // Debug logging (temporary)
        System.out.println(
                "[Addiction] " + player.getName() +
                " used " + drugId +
                " | points=" + state.getPoints()
        );
    }

    /* ======================================================
     * Query helpers
     * ====================================================== */

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
        AddictionConfig.DrugAddictionRule rule =
                config.drugs.get(drugId.toLowerCase());

        if (state == null || rule == null) return false;
        return state.getPoints() >= rule.addictedThreshold;
    }

    /* ======================================================
     * Cure handling (Suboxone, etc.)
     * ====================================================== */

    public static boolean applyCure(Player player, String cureId) {
        if (config == null) return false;

        cureId = cureId.toLowerCase();
        AddictionConfig.CureRule cure = config.cures.get(cureId);
        if (cure == null) return false;

        UUID uuid = player.getUniqueId();
        Map<String, AddictionState> playerAddictions = addictions.get(uuid);
        if (playerAddictions == null) return false;

        boolean used = false;

        for (Map.Entry<String, AddictionState> entry : playerAddictions.entrySet()) {
            String drugId = entry.getKey();
            AddictionState state = entry.getValue();

            if (!cure.allowsDrug(drugId)) continue;

            if (cure.reducePoints > 0) {
                state.removePoints(cure.reducePoints);
            }

            if (cure.blockWithdrawalSeconds > 0) {
                state.blockWithdrawalForSeconds(cure.blockWithdrawalSeconds);
            }

            used = true;
        }

        return used;
    }
}
