# Flyiron's Drugs Plugin — Release Notes v1.1

**Release scope:** commits starting **2026-02-15** and later  
**Previous version:** all changes up to **2026-01-30**

---

## Overview
v1.1 is a major gameplay expansion focused on:

- A full **cannabis strain genetics/progression system**
- A new **/strains GUI** with sorting/pagination improvements
- A complete **placeable bong system** with visual entity models
- **Cannabis growth and durability mechanics** (cart + bong)
- Multiple QoL and persistence fixes for reliability after restart
- Updated public-facing documentation for GitHub and Spigot

---

## Major Features

## 1) Cannabis Strain System (NEW)
Introduced a large strain ecosystem with progression logic and mutations.

### Includes
- Full cannabis strain framework with genetics support
- Expanded strain pool (100+ strains)
- Progression tree balancing and lineage flow updates
- `/strains` command and GUI support

### Notable follow-ups
- Fixed lineage/mutation behavior around fern interactions
- Fixed GUI labels/IDs and naming consistency
- Added tier-based pagination and sorting for browsing large strain lists

---

## 2) Placeable Bongs (NEW)
Added placeable bong items as in-world interactive objects using model/display entities.

### Includes
- Placeable bong item and interaction flow
- Bong usage integrated with drug systems and menu access
- Strain-aware and feedback-oriented usage behavior
- Crafting support via `recipes.yml`

### Iteration/fixes during rollout
- Orientation and rotation logic fixes (including 45-degree snapping)
- Hitbox and break interaction refinements
- Model alignment/scale/texture fixes
- Support/placement rule enforcement and cleanup improvements
- Restart persistence fix (prevented interaction loss after reboot)

---

## 3) New Cannabis Mechanics
Gameplay support systems were added/refined to support long-term use:

- Auto weed growth mechanics (configurable timing)
- Cart durability with action bar feedback
- Configurable bong durability with persistence
- Placement restrictions to valid block surfaces

---

## 4) Balance & System Tweaks
- Sleep cure mapping updated to include meth
- Additional tuning and naming cleanups for consistency

---

## 5) Documentation & Publishing Updates
- Added `description.bbcode` for Spigot page usage
- Refreshed `README.md` and `description.bbcode` with complete command coverage and updated feature documentation

---

## Command/UX Impact in v1.1
Players/admins will notice improved usability around cannabis and bong gameplay:

- `/strains` now supports improved browsing for large strain lists
- Bong placement/use is now a first-class gameplay path
- Action bar messaging improves durability visibility for consumable tools (cart/bong)

---

## Commit Range (v1.1)
From first v1.1 commit:
- `89a5518` — Implement full cannabis strain system with genetics and /strains GUI

Through latest included documentation update:
- `0e0a481` — docs refresh for README + Spigot description

---

## Notes for Server Owners
If you are upgrading from the Jan 30-and-earlier build:

1. Back up your plugin data folder.
2. Let the plugin regenerate any missing/new config files.
3. Review and merge changes for:
   - `strains.yml`
   - `bong.yml`
   - `mechanics.yml`
   - `recipes.yml`
4. Validate permissions/command access for staff and players.
5. Test bong placement and strain browsing in a staging environment before production rollout.
