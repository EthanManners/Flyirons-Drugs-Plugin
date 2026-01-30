# PUBLIC SMP SERVER COMING SOON

# Flyirons Drugs Plugin

Flyirons Drugs is a fork of the original **Norses Drugs** plugin for survival SMP servers.

It adds craftable consumables that apply potion effects, along with a persistent addiction, tolerance, and overdose system. The plugin adds an extra gameplay layer that affects PvP, resource production, logistics, and long-term player decisions.

---

## How This Plugin Is Meant to Be Used

This plugin is designed to work **out of the box**.

All drugs, effects, crafting recipes, addiction behavior, withdrawal rules, and overdose logic are already configured and balanced relative to each other. The config files define the actual gameplay and are not examples or placeholders.

You *can* change the configs, but the default values are intentional and meant to be used as-is.

Best suited for:
- Survival SMP servers  
- PvP-enabled servers  
- Long-running worlds  

---

## Gameplay Overview

### Drug Effects
- Each drug applies a fixed set of potion effects.
- Effects are generally short in duration.
- Buffs are paired with downsides such as slowness, hunger, weakness, nausea, or visibility.

### Addiction & Tolerance
- Repeated use increases tolerance and addiction.
- Addiction persists across sessions.
- Higher addiction increases overdose risk.
- Different drugs have different addiction severity levels.

### Withdrawal
- Withdrawal begins when an addicted player stops using a drug.
- Withdrawal persists until:
  - a cure is used, or
  - the drug is taken again (temporarily blocking withdrawal).
- Most drugs apply **Poison** during withdrawal.
- Cannabis-based drugs apply **Hunger** instead of Poison.

### Overdose
- Overdose chance scales based on addiction level and usage frequency.
- Overdoses can kill the player.
- Death messages indicate which drug caused the overdose.

### Crafting & Supply
- Drugs are crafted using survival resources.
- Many recipes require dangerous, rare, or time-consuming materials.
- Sustained drug use requires ongoing production and logistics.

---

## Drugs

### Heroin
- **Effects:** Strength, high Resistance, Slowness  
- **Addiction:** High  
- **Overdose Risk:** High  
- **Withdrawal:** Poison  
- **Role:** Short-term combat durability with heavy movement penalty
- **Notes:** Strong defensive boost but difficult to fight or escape while using

### Fent
- **Effects:** Strength, Absorption, very high Resistance, extreme Slowness  
- **Addiction:** Extreme  
- **Overdose Risk:** Very High  
- **Withdrawal:** Poison  
- **Role:** Extremely strong damage reduction for a brief window
- **Notes:** Meant to be dangerous and unsustainable without cures or maintenance

### Cocaine
- **Effects:** Speed, Strength, Saturation  
- **Addiction:** High  
- **Overdose Risk:** High  
- **Withdrawal:** Poison  
- **Role:** Short burst combat and movement
- **Notes:** Very short duration, encourages repeated use

### Meth
- **Effects:** Speed, Haste, Weakness  
- **Addiction:** Medium  
- **Overdose Risk:** Medium  
- **Withdrawal:** Poison  
- **Role:** Fast mining and movement at the cost of combat effectiveness

### Molly
- **Effects:** Regeneration, Absorption, Glowing  
- **Addiction:** Low  
- **Overdose Risk:** Low  
- **Withdrawal:** Poison  
- **Role:** Team pushes and sustained fights, reduced stealth

### Shrooms
- **Effects:** Jump Boost, Luck, Nausea  
- **Addiction:** Non-Addictive
- **Overdose Risk:** Non-lethal  
- **Withdrawal:** None
- **Role:** Utility and exploration

### Cannabis (Blunt / Joint / Edible / Cart)
- **Effects:** Resistance, Hunger, Slowness  
- **Addiction:** Low  
- **Overdose Risk:** Non-lethal  
- **Withdrawal:** Hunger  
- **Role:** Low-risk defensive buffs with movement and food penalties  
- **Notes:** Cart is non-stackable; other forms are stackable

### Glue
- **Effects:** Poison, Mining Fatigue  
- **Addiction:** High  
- **Overdose Risk:** High  
- **Withdrawal:** Poison  
- **Role:** Cheap and intentionally bad

---

## Cures

### Enchanted Golden Apple
- **Effect:** Clears all addiction and tolerance
- **Use Case:** Full reset / emergency cure

### Suboxone
- **Effect:** Cures opioid addiction  
- **Applies To:** Heroin, Fent  
- **Notes:** Intended for managing or exiting high-risk opioid dependence

### CBD
- **Effect:** Cures cannabis addiction  
- **Applies To:** Blunt, Joint, Edible, Cart  
- **Notes:** Removes hunger-based withdrawal

### Sleeping
- **Effect:** Clears stimulant addiction  
- **Applies To:** Cocaine, Meth  
- **Notes:** Requires actual sleep, not just time passing

---

## Resource Pack (Recommended)

The repo includes:

**`RECOMMENDEDResourcePack.zip`**

This resource pack only adds **custom textures** for drug items.  
It is optional, but recommended for visual clarity.

---

## Installation

1. Drop the plugin JAR into your `plugins` folder.
2. (Recommended) Install the included resource pack.
3. Start or restart the server.

---

## Credits

- Original Plugin: **Norses Drugs**
- Fork and Balance Changes: **Flyiron**
