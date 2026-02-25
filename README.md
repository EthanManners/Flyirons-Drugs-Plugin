# Flyiron's Drugs Plugin (DrugsV2)

Discord: https://discord.gg/SaxTyAFHV9  
Public SMP: mc.swanky.wtf

Flyiron's Drugs Plugin is a heavily expanded survival/PvP gameplay system built around **powerful consumables**, **tolerance**, **addiction**, **withdrawal**, **overdose risk**, **cures**, **cannabis strains**, **bong/cart durability**, and **player achievements**.

This is a fork of the original Norses Drugs plugin and is designed to be playable immediately with the provided defaults.

---

## What’s New / Current Feature Set

Compared to a basic consumable-effects plugin, this version includes:

- Persistent, drug-specific tolerance and addiction tracking.
- Configurable overdose behavior (global, per-drug, staged, and random modes).
- Recovery systems (Suboxone, CBD, sleep-based cure logic, enchanted golden apple).
- Cannabis-focused systems:
  - Large strain registry with rarity and mutation weighting.
  - Plant growth timing controls.
  - `/strains` GUI index.
- Bong support:
  - Craftable bong item.
  - Configurable cooldown.
  - Configurable durability/uses.
- Cart durability/uses mechanic.
- Achievement system + GUI + configurable triggers.
- PlaceholderAPI expansion for tolerance/effectiveness placeholders.
- Admin tooling for reloading configs, purging player data, listing drugs, and resetting overdose counters.

---

## Quick Start (Server Admin)

1. Build or download the plugin JAR.
2. Put the JAR in `plugins/`.
3. Start the server once to generate config files.
4. (Recommended) Install `RECOMMENDEDResourcePack.zip` for custom textures.
5. Review generated config files and adjust balance if needed.
6. Use `/drugs reload` after config edits.

---

## How to Use the Plugin (Gameplay + Admin Guide)

## Player Flow

1. Craft or receive a drug item.
2. Consume it to gain short-term buffs and tradeoff effects.
3. Repeated use increases tolerance; potency drops over time.
4. Continued use can cause addiction.
5. If addicted, withdrawal effects begin after the configured delay.
6. Recover with the matching cure, sleep (for configured stimulants), or golden apple reset.
7. Track your status with `/tolerance`.
8. Browse cannabis strain info with `/strains`.
9. Track progression through `/drugs achievements`.

## Admin Flow

1. Give test items with `/drugs give`.
2. Validate config edits with `/drugs reload`.
3. Audit loaded drugs with `/drugs list`.
4. Reset problem accounts with `/drugs purge <player>`.
5. Reload overdose settings independently with `/drugs overdose reload`.
6. Clear overdose counters only with `/drugs overdose reset <player>`.

---

## Commands

## Player Commands

- `/drugs`  
  Opens the main drug menu GUI.

- `/drugs help`  
  Shows context-sensitive help based on permissions.

- `/tolerance`  
  Displays each registered drug tolerance level, max, and current potency %.

- `/strains`  
  Opens the cannabis strain index GUI.

- `/drugs achievements`  
  Opens the achievements GUI.

## Admin Commands

- `/drugs give <player> <drugId|cureId|bong|all> [amount]`  
  Gives one specific item type or all registered types.

- `/drugs purge <player>`  
  Resets all tolerance and overdose counts for a player.

- `/drugs reload`  
  Reloads plugin configs and registries.

- `/drugs list`  
  Prints all loaded registered drugs and lore in chat.

- `/drugs overdose reload`  
  Reloads overdose settings file.

- `/drugs overdose reset <player>`  
  Resets overdose attempt counters for a player.

- `/drugs achievements toggle`  
  Admin helper command that instructs editing `achievement_settings.yml` (`enabled`) and then using `/drugs reload`.

---

## Permissions

- `drugs.menu` (default: true)
- `drugs.give` (default: op)
- `drugs.tolerance` (default: true)
- `drugs.strains` (default: true)
- `drugs.achievements` (default: true)
- `drugs.admin.reload` (default: op)
- `drugs.admin.purge` (default: op)
- `drugs.admin.list` (default: op)
- `drugs.admin.achievements` (default: op)
- `drugs.admin.overdose` (default: op)

---

## Included Drug Categories

- Opioids / high-risk: `heroin`, `fent`, `glue`
- Stimulants / combat-mobility: `cocaine`, `meth`, `molly`
- Cannabis variants: `blunt`, `joint`, `edible`, `cart`
- Utility/psychedelic: `shrooms`

Each drug profile is fully configurable in `config.yml` (material, display name, lore, potion effects).

---

## Recovery / Cure System

Defined in `addiction.yml` and craftable through `recipes.yml` where enabled:

- **Suboxone**: cures configured opioid-related addictions.
- **CBD**: cannabis-focused cure behavior.
- **Sleep**: configured as cure logic for selected drugs.
- **Enchanted Golden Apple**: full addiction reset behavior.

Withdrawal behavior (effect types, timings, point decay) is per-drug and configurable.

---

## Cannabis Systems

- `strains.yml` ships with an extensive strain tree, rarity, and mutation weights.
- `mechanics.yml` controls cannabis growth timing.
- `bong.yml` controls bong enable state, base-drug behavior, cooldown, and bong item settings.
- `mechanics.yml` also controls cart and bong durability uses.

---

## Overdose System

`overdose.yml` supports:

- Global enable/disable.
- Attempt threshold.
- Per-drug vs global tracking mode.
- Attempt expiry windows.
- Broadcast toggles.
- Default fallback effects.
- Drug-specific overrides.
- Optional staged progression and random effect pools.

Default config includes non-lethal confusion/nausea overdose behavior for cannabis variants while preserving lethal behavior pathways for other drugs depending on configuration.

---

## Achievement System

Achievements are split into:

- `achievement_settings.yml` (system toggles + notifications + quick defaults)
- `achievements.yml` (trigger definitions and custom achievements)

Supported trigger styles include first use, all drugs used, max tolerance behavior, crafting-based triggers, use-count triggers, and overdose outcomes.

---

## PlaceholderAPI Support

If PlaceholderAPI is installed, this plugin registers `%drugs_...%` placeholders:

- `%drugs_<drugid>%` → current tolerance
- `%drugs_<drugid>_max%` → max tolerance
- `%drugs_<drugid>_effectiveness%` → potency percentage

---

## Files and Configuration Map

- `config.yml` → drug item/effect definitions
- `recipes.yml` → crafting recipes
- `tolerance.yml` → tolerance caps, decay delay, scaling
- `addiction.yml` → addiction points, withdrawal rules, cures
- `overdose.yml` → overdose effects and policy
- `strains.yml` → cannabis strains and mutation graph
- `bong.yml` → bong behavior and item settings
- `mechanics.yml` → growth + durability mechanics
- `achievement_settings.yml` → achievement master toggles
- `achievements.yml` → achievement definitions/triggers

---

## Compatibility

- API version: `1.21`
- Soft dependency: PlaceholderAPI

---

## Credits

- Original base plugin: **Norses Drugs**
- Fork + continued systems work/balance: **Flyiron**
