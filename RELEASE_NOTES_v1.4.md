# Flyiron's Drugs Plugin — Release Notes v1.4

**Release scope:** strain effect overhaul, admin strain tooling, and cannabis UX updates  
**Previous version:** v1.3

---

## Overview
v1.4 focuses on a full cannabis strain behavior pass centered on clarity, balance, and admin usability.

### Headline changes
- Strains now use explicit, per-strain effect entries in config.
- Cannabis strain UI now shows each strain’s actual effects.
- Weed product lore now shows strain-specific effects for easier player understanding.
- Added admin `/strains give <player> <strain-id>` support.
- Reggie now behaves as an intentionally unknown/low-tier option with random debuff behavior.

---

## New Features

## 1) Admin strain give command
Added a new admin subcommand:

- `/strains give <player> <strain-id>`

### Details
- Validates player target and strain ID.
- Gives the target a strain plant item for testing or staff workflows.
- Includes tab-completion for:
  - `give`
  - online player names
  - available strain IDs
- Controlled by permission:
  - `drugs.admin.strains.give`

---

## 2) Strain effects shown in GUI and item lore
Strain presentation has been updated to make effects readable in-game.

### GUI changes (`/strains`)
- Each strain card now shows its configured effects.
- Mutation weights are no longer shown in the GUI preview block.
- Reggie displays unknown/random-debuff messaging.

### Item lore changes
- Cannabis products now display strain + effect details in lore.
- Strain plant items also include rarity/mutation chance/effect information.
- Reggie lore intentionally displays “unknown effects” style messaging.

---

## 3) Strain effect balance and rarity mapping
Strain effects were rebalanced by rarity band using curated buff/debuff pools.

### Design rules applied
- Every non-reggie strain includes at least one debuff.
- Potency and durations scale from common (lowest) to legendary (highest).
- Higher-impact effects are constrained to higher tiers.
- `ABSORPTION` is rare-limited (only a small subset of rare strains).

### Reggie behavior
- Reggie now applies a random debuff on each use.
- Debuff is selected from:
  - slowness, weakness, nausea, hunger, blindness, darkness, mining fatigue
- Duration is randomized up to 60 seconds per use.

---

## Config Notes

## `strains.yml`
- Uses explicit per-strain `effects` entries (`type`, `duration`, `amplifier`).
- Includes rarity-informed default effect pools.
- Maintains mutation chance tuning introduced in prior balancing passes.

---

## Permissions

New/updated permission usage in v1.4:

- `drugs.admin.strains.give` (default: op)

---

## Upgrade Notes for Server Owners

1. Back up your plugin folder before deploying v1.4.
2. Merge `strains.yml` carefully if you keep custom strains.
3. Validate command permissions for moderators/admins (`drugs.admin.strains.give`).
4. Spot-check in-game:
   - `/strains` GUI effect display
   - strain plant lore
   - crafted cannabis product lore
   - reggie random debuff behavior
5. If your server had custom lore templates, re-verify line ordering after update.

---

## Recommended Smoke Test Checklist

- Open `/strains` and verify effect lists show for each rarity.
- Use `/strains give <player> <strain-id>` as admin.
- Craft at least one cannabis product from each rarity tier and confirm lore includes effects.
- Consume reggie repeatedly and confirm random debuff rotation.
- Consume uncommon/rare/legendary strains and confirm stronger average effect profile by tier.
