# Flyiron's Drugs Plugin — Player Wiki

> This page is for **players**.
> 
> - If you're a **developer**, use `README.md`.
> - If you're a **server owner/admin**, use `description.bbcode` and server docs.

---

## 1) What This System Is

Flyiron's Drugs adds a long-term risk/reward layer to survival gameplay.

You can take items for strong short-term buffs, but repeated use can cause:

- **Tolerance** (your doses get weaker)
- **Addiction** (you need maintenance or cure)
- **Withdrawal** (persistent negative effects)
- **Overdose** (dangerous effects or death depending on drug)

This means your decisions matter over time, not just in one fight.

---

## 2) Quick Start for New Players

1. Open the drug menu with `/drugs`.
2. Try a drug that matches your activity (combat, mining, travel, etc.).
3. Watch your tolerance with `/tolerance`.
4. If you rely on a drug too often, plan for cure/recovery.
5. Use `/drugs achievements` to track progress milestones.
6. If your server uses strains, browse them with `/strains`.

---

## 3) Player Commands

### Core Commands

- `/drugs` → Opens the main menu.
- `/drugs help` → Shows command help.
- `/tolerance` → Shows your tolerance and current potency per drug.
- `/strains` → Opens cannabis strain browser.
- `/drugs achievements` → Opens achievements GUI.

> Tip: `/tolerance` is your most important survival command if you use drugs often.

---

## 4) Core Mechanics (How It Actually Works)

## A) Drug Effects

Every drug has:

- **Upsides** (speed, strength, resistance, utility, etc.)
- **Downsides** (slowness, weakness, hunger, nausea, glowing, poison, etc.)

Most are built as tradeoffs, not pure power.

## B) Tolerance

When you use the same drug repeatedly, your body adapts:

- Effects become weaker at higher tolerance.
- At very high tolerance, doses may give little value.
- Tolerance decays slowly over time if you stop.

Use `/tolerance` to check where your potency currently sits.

## C) Addiction

If you push usage too hard, you can become addicted:

- Addiction is tracked per drug.
- Higher-risk drugs can hook you faster.
- Addiction persists until you recover.

## D) Withdrawal

Once addicted, stopping can trigger withdrawal:

- For many hard drugs, withdrawal applies **Poison**.
- For cannabis-type drugs, withdrawal is usually **Hunger** based.
- Withdrawal does not just disappear instantly; you need recovery.

## E) Overdose

Using at dangerous levels can trigger overdose behavior:

- Some drugs are more lethal than others.
- Overdose danger rises with abusive patterns.
- Repeated risky behavior can lead to severe punishment or death.

---

## 5) Drug-by-Drug Guide

Below is the player-facing purpose of each drug family.

## Heroin

- **Main upsides:** Strong combat durability (Strength + high Resistance)
- **Main downsides:** Significant Slowness
- **Playstyle:** Short heavy-fight windows / tanking
- **Risk profile:** High addiction pressure, high overdose danger
- **Use case:** Defensive pushes where mobility is less important

## Fent

- **Main upsides:** Very high survivability burst (Resistance + Absorption + Strength)
- **Main downsides:** Extreme Slowness
- **Playstyle:** Last-stand or forced hold positions
- **Risk profile:** Very high addiction and overdose risk
- **Use case:** Emergency durability when retreating is not possible

## Cocaine

- **Main upsides:** Very fast burst speed and combat tempo
- **Main downsides:** Short windows encourage repeated re-dosing
- **Playstyle:** Chase, disengage, burst skirmishes
- **Risk profile:** High abuse potential due to short-duration loop
- **Use case:** High-tempo PvP movement and quick objective runs

## Meth

- **Main upsides:** Speed + Haste (excellent for mining and movement)
- **Main downsides:** Weakness hurts direct fighting
- **Playstyle:** Resource rush / mobility utility
- **Risk profile:** Medium addiction pressure; still dangerous if spammed
- **Use case:** Mining sessions and map traversal

## Molly

- **Main upsides:** Regeneration + Absorption
- **Main downsides:** Glowing (you are easier to track)
- **Playstyle:** Team pushes and sustained engagements
- **Risk profile:** Lower than heavy drugs, but still not free
- **Use case:** Group combat where visibility tradeoff is acceptable

## Shrooms

- **Main upsides:** Jump/Luck utility
- **Main downsides:** Heavy visual distortion (Nausea)
- **Playstyle:** Utility / fun / movement flavor
- **Risk profile:** Lower lethality profile than hard options
- **Use case:** Situational movement + exploration gimmick plays

## Cannabis Family (Blunt / Joint / Edible / Cart)

Shared identity:

- **Upsides:** Resistance-focused survivability
- **Downsides:** Hunger + Slowness style tradeoff
- **Withdrawal style:** Hunger-based
- **Risk profile:** Usually lower lethality path than hard drugs

Variant feel:

- **Blunt:** Standard baseline
- **Joint:** Similar profile with its own pacing
- **Edible:** Slower/more sustained style
- **Cart:** Convenience with durability-limited usage

## Glue

- **Main upsides:** Very little upside, intentionally poor choice
- **Main downsides:** Poison + Mining Fatigue
- **Playstyle:** Budget/high-risk meme route
- **Risk profile:** High downside, high addiction/overdose pressure
- **Use case:** Usually avoid unless roleplay/challenge scenario

---

## 6) Recovery Guide (How to Get Clean)

If you are addicted or in withdrawal, recovery is a gameplay decision.

## Recovery Paths

- **Suboxone**: opioid-style recovery path (server-configured targets)
- **CBD**: cannabis recovery path
- **Sleep**: can clear configured stimulant addiction paths
- **Enchanted Golden Apple**: broad emergency reset route

## Practical Recovery Strategy

1. Stop panic-dosing if tolerance is maxed and value is low.
2. Check `/tolerance` and identify your worst offenders.
3. Prioritize getting the matching cure route.
4. Build recovery into your logistics (don’t wait until crisis).

---

## 7) Strains, Bongs, and Carts

## Cannabis Strains

If your server enables strain systems:

- Use `/strains` to browse available strains.
- Different strains can influence performance style.
- Strain systems reward players who engage with progression over time.

## Bongs

- Bongs are interactive and can be used repeatedly.
- They have durability/usage limits.
- They are useful for players who commit to cannabis gameplay loops.

## Carts

- Cart use is durability-based.
- Good for convenience, but not infinite.
- Track your usage and replace before critical moments.

---

## 8) Survival Meta: What Behavior This System Creates

For survival servers, this system naturally creates:

- **Supply chains** (you need sustained ingredient flow)
- **Trade** (cures and specialty consumables become valuable)
- **Territorial pressure** (resource zones matter)
- **Timing-based PvP** (spikes are strong, spam is punished)
- **Long-term consequences** (bad habits create real setbacks)

Players who plan logistics outperform players who only chase short-term buffs.

---

## 9) Common Mistakes (And Fixes)

## Mistake: Spamming one drug only
- **Result:** High tolerance, weak effects, more danger.
- **Fix:** Rotate strategies, recover, and lower dependence.

## Mistake: Ignoring withdrawal until a fight
- **Result:** You enter combat with hidden penalties.
- **Fix:** Handle recovery before important PvP windows.

## Mistake: No cure stockpile
- **Result:** One bad streak creates long downtime.
- **Fix:** Keep recovery items in base kits and travel kits.

## Mistake: Treating high-risk drugs as free buffs
- **Result:** Overdose chain and potential death.
- **Fix:** Use heavy options only for specific high-value moments.

---

## 10) Suggested Player Progression

### Early Game
- Learn `/drugs`, `/tolerance`, `/drugs achievements`.
- Test lower-commitment options to understand your server's pacing.

### Mid Game
- Build ingredient/cure access.
- Start using drugs for specific tasks (not constant uptime).
- Explore `/strains` and cannabis progression if enabled.

### Late Game
- Optimize high-impact windows (raids, sieges, key fights).
- Maintain cure reserves and durability items.
- Treat drug usage as a strategic resource, not a default state.

---

## 11) Final Tips

- Track your tolerance often.
- Carry recovery options.
- Use heavy-risk drugs intentionally, not habitually.
- Build team logistics around supply + cure reliability.

If you play this system like a long-term strategy game, you’ll consistently outperform players who play it like a one-click buff plugin.
