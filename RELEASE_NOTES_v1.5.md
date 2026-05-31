# Flyiron's Drugs Plugin — Release Notes v1.5

**The LSD + Salvia update — plus a full rebrand to Flyiron's Drugs and a versioning cleanup.**

This release adds two new hallucinogen-style drugs and rebrands every player- and
admin-facing surface from the old internal "DrugsV2" identity to **Flyiron's Drugs**, with
the version now driven from a single source.

> **Scope note:** these notes cover what actually changed in this release, verified against
> the repository. They intentionally do **not** reconstruct the plugin's earlier (and
> inconsistent) version history.

---

## New Drugs

### LSD *(new)*
- **Item:** Purple Dye displayed as `&5LSD`. Registered as a normal drug, so it appears in
  the `/drugs` menu and works with `/drugs give <player> lsd`.
- **Recipe (shaped):** 6× Paper, 2× Spore Blossom, 1× Eye of Ender
  ```
  P P P
  S E S      P = Paper   S = Spore Blossom   E = Eye of Ender
  P P P
  ```
- **Effect:** after a short delayed onset (~5s), the block you're standing on appears to
  turn into a **nether portal that follows you** wherever you walk for ~3 minutes, with
  Nausea applied for the duration. The portal is a **client-side hallucination only** —
  sent per-player via block changes, so **no real blocks are placed or modified** (no world
  damage, no griefing). It clears automatically on expiry, death, or logout.
- **Config:** `config.yml` → `lsd.delay` (default `5`s), `lsd.duration` (default `180`s).

### Salvia *(new)*
- **Item:** Fern displayed as `&2Salvia`. Registered as a normal drug (`/drugs` menu and
  `/drugs give <player> salvia`).
- **Recipe (shaped):** 2× Leaf Litter, 2× Fern, 1× Dirt
  ```
  . L .
  F D F      L = Leaf Litter   F = Fern   D = Dirt
  . L .
  ```
- **Effect:** after a short delayed onset (~3s), you **disguise as a dirt block** for ~45s —
  you turn invisible and a dirt block model tracks your position, so other players see an
  ordinary dirt block where you're standing. The invisibility and the disguise model are
  always cleaned up on expiry, death, or logout.
- **Config:** `config.yml` → `salvia.delay` (default `3`s), `salvia.duration` (default `45`s).

Both ship as **non-lethal, low-abuse** consumables (per item lore) and **do not** define
dedicated `tolerance.yml` / `addiction.yml` / `overdose.yml` entries — they inherit the
system defaults. Add per-drug entries later if you want them on tolerance/addiction/overdose
tracks.

---

## Rebrand: DrugsV2 → Flyiron's Drugs

Everything a player or admin sees is now **Flyiron's Drugs** branded. The internal code
identity (Java package/class `com.drugs.DrugsV2`, Maven `artifactId`) is intentionally left
unchanged.

- **Plugin name** is now `FlyironsDrugs`, so the **data folder is `plugins/FlyironsDrugs/`**.
- **Console/log prefix** is now `[FlyironsDrugs]` (was `[DrugsV2]`).
- **In-game text** rebranded: the `/drugs help` header, the config-reload confirmation, and
  the enable/disable console messages.
- A **`description`** was added to `plugin.yml` (shown in `/version` and `/plugins`).
- **Unchanged:** the commands `/drugs`, `/tolerance`, `/strains`, and the PlaceholderAPI
  identifier (`%drugs_...%`).

## Versioning

- Version is now **1.5.0**, defined once in `pom.xml` and filtered into `plugin.yml` via
  `${project.version}` — no more drift between the pom, the manifest, and the docs.
- The built jar is **`FlyironsDrugsv1.5.jar`**.
- Documentation updated to match; the stale `v1.3` feature marker was removed.

---

## ⚠️ Upgrade Notes (existing servers)

The data folder moved and does **not** auto-migrate:

1. Stop the server.
2. Replace the old jar with **`FlyironsDrugsv1.5.jar`** (delete the old `DrugsV2` jar).
3. **`mv plugins/DrugsV2 plugins/FlyironsDrugs`** — this moves all configs plus the `data/`
   subtree (achievements, addiction data, weed farms, bong & plant registries) in one step.
4. Start, and confirm the console enables it as **Flyiron's Drugs** with your configs/data
   intact.

**Existing drug items keep working.** When an older item lacks the current data tag it is
matched by display name and silently re-tagged on next use. Two minor edge effects apply to
that first re-tag of *pre-update* items: a cannabis item's specific strain tag and a
cart/bong's remaining-durability counter won't carry over (the item returns to its default).
Newly crafted items are unaffected.

---

## Compatibility *(unchanged)*

- Paper **1.21**, Java **21**
- PlaceholderAPI — soft dependency
