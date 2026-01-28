package com.drugs;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages addiction state, withdrawal, and cures for players.
 */
public class AddictionManager {

    private static final Map<UUID, Map<String, AddictionState>> addictionData = new ConcurrentHashMap<>();
    private static File dataFile;
    private static FileConfiguration dataConfig;

    private static int heartbeatTaskId = -1;
    private static int autosaveTaskId = -1;

    public static void init(Plugin plugin) {
        loadData(plugin.getDataFolder());
        startHeartbeat(plugin);
        startAutosave(plugin);
    }

    public static void reload(Plugin plugin) {
        stopTasks();
        startHeartbeat(plugin);
        startAutosave(plugin);
    }

    public static void shutdown() {
        saveData();
        stopTasks();
    }

    private static void startHeartbeat(Plugin plugin) {
        AddictionSettings settings = AddictionConfigLoader.getSettings();
        if (!settings.isEnabled()) return;

        heartbeatTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(
                plugin,
                AddictionManager::processHeartbeat,
                settings.getHeartbeatTicks(),
                settings.getHeartbeatTicks()
        );
    }

    private static void startAutosave(Plugin plugin) {
        autosaveTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(
                plugin,
                AddictionManager::saveData,
                20L * 300L,
                20L * 300L
        );
    }

    private static void stopTasks() {
        if (heartbeatTaskId != -1) {
            Bukkit.getScheduler().cancelTask(heartbeatTaskId);
            heartbeatTaskId = -1;
        }
        if (autosaveTaskId != -1) {
            Bukkit.getScheduler().cancelTask(autosaveTaskId);
            autosaveTaskId = -1;
        }
    }

    private static void processHeartbeat() {
        AddictionSettings settings = AddictionConfigLoader.getSettings();
        if (!settings.isEnabled()) return;

        long now = System.currentTimeMillis();

        for (Player player : Bukkit.getOnlinePlayers()) {
            Map<String, AddictionState> playerData = addictionData.get(player.getUniqueId());
            if (playerData == null) continue;

            for (Map.Entry<String, AddictionState> entry : new HashMap<>(playerData).entrySet()) {
                String drugId = entry.getKey();
                AddictionState state = entry.getValue();
                AddictionDrugProfile profile = AddictionConfigLoader.getDrugProfile(drugId);
                if (profile == null) continue;

                applyDecay(state, profile, now);
                processWithdrawal(player, state, profile, now, settings);
            }
        }
    }

    private static void applyDecay(AddictionState state, AddictionDrugProfile profile, long now) {
        if (!profile.isDecayEnabled() || state.getPoints() <= 0) {
            return;
        }

        long lastDecay = state.getLastDecayMillis();
        if (lastDecay == 0L) {
            state.setLastDecayMillis(now);
            return;
        }

        long elapsedMillis = now - lastDecay;
        if (elapsedMillis <= 0) return;

        double minutesElapsed = elapsedMillis / 60000.0;
        double decayAmount = minutesElapsed * profile.getDecayPointsPerMinute();
        if (decayAmount <= 0) return;

        state.setPoints(state.getPoints() - decayAmount);
        state.setLastDecayMillis(now);
    }

    private static void processWithdrawal(Player player,
                                          AddictionState state,
                                          AddictionDrugProfile profile,
                                          long now,
                                          AddictionSettings settings) {
        boolean addicted = profile.isAddictive() && state.getPoints() >= profile.getAddictedAtPoints();
        boolean blocked = now < state.getWithdrawalBlockedUntilMillis();

        if (!addicted || blocked) {
            if (state.isWithdrawalActive()) {
                clearWithdrawalEffects(player, profile);
                state.setWithdrawalActive(false);
            }
            return;
        }

        long lastUse = state.getLastUseMillis();
        long threshold = profile.getWithdrawalAfterSeconds() * 1000L;

        if (threshold > 0 && now - lastUse >= threshold) {
            applyWithdrawalEffects(player, profile, settings.getInfiniteDurationTicks());
            state.setWithdrawalActive(true);
        } else if (state.isWithdrawalActive()) {
            clearWithdrawalEffects(player, profile);
            state.setWithdrawalActive(false);
        }
    }

    public static void onDrugUse(Player player, String drugId) {
        AddictionSettings settings = AddictionConfigLoader.getSettings();
        if (!settings.isEnabled()) return;

        AddictionDrugProfile profile = AddictionConfigLoader.getDrugProfile(drugId);
        if (profile == null || !profile.isAddictive()) return;

        AddictionState state = getOrCreateState(player.getUniqueId(), drugId);
        state.setPoints(state.getPoints() + profile.getPointsPerUse());
        long now = System.currentTimeMillis();
        state.setLastUseMillis(now);
        state.setLastDecayMillis(now);
        state.setWithdrawalActive(false);
    }

    public static boolean applyCure(Player player, CureProfile cure) {
        AddictionSettings settings = AddictionConfigLoader.getSettings();
        if (!settings.isEnabled() || cure == null || !cure.isEnabled()) return false;

        boolean affected = false;
        long now = System.currentTimeMillis();
        boolean curesAll = cure.getCures().stream().anyMatch(entry -> entry.equalsIgnoreCase("*"));

        for (Map.Entry<String, AddictionDrugProfile> entry : AddictionConfigLoader.getDrugProfiles().entrySet()) {
            String drugId = entry.getKey();
            AddictionDrugProfile profile = entry.getValue();

            if (!curesAll && cure.getCures().stream().noneMatch(id -> id.equalsIgnoreCase(drugId))) {
                continue;
            }

            AddictionState state = getOrCreateState(player.getUniqueId(), drugId);
            if (cure.isClearsPoints()) {
                state.setPoints(0);
            } else if (cure.getReducePoints() > 0) {
                state.setPoints(state.getPoints() - cure.getReducePoints());
            }

            if (cure.getBlockWithdrawalSeconds() > 0) {
                state.setWithdrawalBlockedUntilMillis(now + cure.getBlockWithdrawalSeconds() * 1000L);
            }

            clearWithdrawalEffects(player, profile);
            state.setWithdrawalActive(false);
            affected = true;
        }

        return affected;
    }

    public static void handleMilkConsumed(Player player) {
        AddictionSettings settings = AddictionConfigLoader.getSettings();
        if (!settings.isEnabled() || settings.isMilkClearsWithdrawal()) return;

        Bukkit.getScheduler().runTaskLater(DrugsV2.getInstance(), () -> {
            long now = System.currentTimeMillis();
            Map<String, AddictionState> stateMap = addictionData.get(player.getUniqueId());
            if (stateMap == null) return;
            for (Map.Entry<String, AddictionState> entry : stateMap.entrySet()) {
                AddictionDrugProfile profile = AddictionConfigLoader.getDrugProfile(entry.getKey());
                if (profile == null) continue;
                processWithdrawal(player, entry.getValue(), profile, now, settings);
            }
        }, 1L);
    }

    private static void applyWithdrawalEffects(Player player, AddictionDrugProfile profile, int duration) {
        for (WithdrawalEffect effect : profile.getWithdrawalEffects()) {
            PotionEffect potion = new PotionEffect(effect.getType(), duration, effect.getAmplifier(), true, true, true);
            player.addPotionEffect(potion);
        }
    }

    private static void clearWithdrawalEffects(Player player, AddictionDrugProfile profile) {
        for (WithdrawalEffect effect : profile.getWithdrawalEffects()) {
            player.removePotionEffect(effect.getType());
        }
    }

    private static AddictionState getOrCreateState(UUID uuid, String drugId) {
        Map<String, AddictionState> playerData = addictionData.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
        return playerData.computeIfAbsent(drugId.toLowerCase(), key -> new AddictionState(0, 0L, 0L, 0L));
    }

    private static void loadData(File dataFolder) {
        if (dataFile == null) {
            dataFile = new File(dataFolder, "addiction_data.yml");
        }
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                Bukkit.getLogger().severe("[DrugsV2] Failed to create addiction_data.yml: " + e.getMessage());
            }
        }

        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        addictionData.clear();

        ConfigurationSection playersSection = dataConfig.getConfigurationSection("players");
        if (playersSection == null) return;

        for (String uuidKey : playersSection.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidKey);
            } catch (IllegalArgumentException e) {
                continue;
            }

            ConfigurationSection playerSection = playersSection.getConfigurationSection(uuidKey);
            if (playerSection == null) continue;

            Map<String, AddictionState> playerMap = new ConcurrentHashMap<>();
            for (String drugId : playerSection.getKeys(false)) {
                ConfigurationSection drugSection = playerSection.getConfigurationSection(drugId);
                if (drugSection == null) continue;

                double points = drugSection.getDouble("points", 0);
                long lastUse = drugSection.getLong("last_use", 0L);
                long blockedUntil = drugSection.getLong("withdrawal_blocked_until", 0L);
                long lastDecay = drugSection.getLong("last_decay", 0L);

                playerMap.put(drugId.toLowerCase(), new AddictionState(points, lastUse, blockedUntil, lastDecay));
            }

            addictionData.put(uuid, playerMap);
        }
    }

    public static void saveData() {
        if (dataFile == null) return;

        FileConfiguration config = new YamlConfiguration();
        ConfigurationSection playersSection = config.createSection("players");

        for (Map.Entry<UUID, Map<String, AddictionState>> playerEntry : addictionData.entrySet()) {
            ConfigurationSection playerSection = playersSection.createSection(playerEntry.getKey().toString());
            for (Map.Entry<String, AddictionState> drugEntry : playerEntry.getValue().entrySet()) {
                AddictionState state = drugEntry.getValue();
                ConfigurationSection drugSection = playerSection.createSection(drugEntry.getKey());
                drugSection.set("points", state.getPoints());
                drugSection.set("last_use", state.getLastUseMillis());
                drugSection.set("withdrawal_blocked_until", state.getWithdrawalBlockedUntilMillis());
                drugSection.set("last_decay", state.getLastDecayMillis());
            }
        }

        try {
            config.save(dataFile);
        } catch (IOException e) {
            Bukkit.getLogger().severe("[DrugsV2] Failed to save addiction_data.yml: " + e.getMessage());
        }
    }
}
