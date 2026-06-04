# FaunaReborn Release / Marketplace Copy

## Canonical Release Title
FaunaReborn 1.3.1 - Chicken Hostility Persistence Fix

## Release Notes Copy

### Chicken Hostility Persistence Fix
This patch completes the chicken hostility natural-origin fix so natural vs non-natural classification stays correct across reloads, server restarts, and lifecycle transitions.

- Spawn-origin classification is now persisted and restored consistently for chickens.
- Chickens born from eggs or breeding keep the correct natural-lifecycle classification after growing into adults.
- Spawn eggs and mob spawners remain excluded from natural-lifecycle hostility checks.
- Legacy configuration alias migration now also covers `entities/chicken.yml`:
  - `activation.only-natural`
  - `activation.natural-spawns-only`

### Compatibility Notes
- Existing installations keep working with legacy activation keys while migrating forward safely.
- Hot reload and normal startup now align on the same chicken-origin compatibility path.

## Marketplace Long Description Add-on

FaunaReborn 1.3.1 strengthens one of the most important gameplay consistency paths in the plugin:

- Chicken hostility now preserves natural vs non-natural spawn origin across restarts and reloads.
- Egg-born and breeding-born chickens remain correctly eligible when they mature into adults.
- Spawn eggs and mob spawner chickens stay excluded from natural-lifecycle hostility logic.
- Legacy config users are migrated safely from `activation.only-natural` to `activation.natural-spawns-only`, including entity-level chicken config coverage.

## Marketplace Short Description

Hostile-fauna AI plugin for Paper/Folia with hardened chicken hostility persistence, safe legacy config migration, and consistent natural-spawn classification.

## Alternate Short Descriptions

1. "Turn passive mobs hostile with premium AI for Paper/Folia, now with safer chicken-origin persistence across reloads and restarts."
2. "Configurable hostile-animal encounters for Paper/Folia with hardened chicken lifecycle classification and legacy config compatibility."
