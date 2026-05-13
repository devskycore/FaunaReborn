# FaunaReborn

> Turn passive farm mobs into dynamic, configurable, performance-safe hostile encounters.

FaunaReborn is a premium-style Paper/Folia plugin that transforms chickens, cows and pigs into intelligent hostile entities with environmental scaling, social alert behavior, and server-friendly safeguards.

## About

FaunaReborn is built for servers that want more tension, replayability and survival depth without replacing vanilla identity.

- Native Paper support (`1.21+` API).
- Folia-compatible architecture.
- Modular design per species (Chicken, Cow, Pig).
- Production-focused config layout with world filtering and hard safety caps.

## Key Features

### Combat AI and Provocation Systems
- **Chicken Hostility Engine** with multi-attacker control, threat decay, line-of-sight logic, and configurable movement behavior.
- **Cow Milk Provocation**: milking can trigger warning, chase, charge and attack phases.
- **Pig Rod Provocation**: fishing-rod interaction can trigger pig retaliation and charge behavior.
- **Resource Territoriality** for chickens/cows/pigs when players repeatedly pick up species-related drops.

### Social and Group Behavior
- **Social Alert Propagation** lets nearby mobs join aggression events.
- Configurable responder radius, join cooldowns, and maximum responders.
- Optional adult-only response behavior.

### Environment-Based Aggression Modifiers
- Rain, thunderstorm and full-moon behavior modifiers.
- Night combo profiles (night+rain, night+storm, night+full moon).
- Tunable multipliers for aggression, detection, damage, speed and persistence.

### Smart Targeting and Protection
- Weighted target scoring (health, distance, threat, line of sight).
- Retarget cooldown and multi-candidate requirements.
- Ignore filters for adventure mode, invisibility, vanish and god-mode setups.

### Performance and Stability
- Global/chunk processing caps to avoid spikes.
- Tick-level processing limits.
- Cache-based environment context updates.
- Built with production operation in mind for long-running servers.

### Admin UX and Control
- In-game **GUI toggles** for entity modules.
- Live **reload workflow** without restarting the server.
- Config-driven world filtering (`ALL`, `WHITELIST`, `BLACKLIST`).

## Commands

| Command | Description | Permission |
|---|---|---|
| `/fauna reload` | Reloads plugin configuration and modules. | `faunareborn.command.reload` |
| `/fauna gui` | Opens the FaunaReborn management GUI. | `faunareborn.command.gui` |

### Aliases
- `/faunareborn`
- `/fr`

## Permissions

| Permission | Description | Default |
|---|---|---|
| `faunareborn.command.reload` | Allows `/fauna reload`. | `op` |
| `faunareborn.reload` | Legacy alias for reload access. | `op` |
| `faunareborn.admin` | Admin alias for reload/management access. | `op` |
| `faunareborn.command.gui` | Allows `/fauna gui`. | `op` |

## Requirements

- **Server software**: Paper (recommended)
- **Minecraft API target**: `1.21`
- **Folia**: Supported
- **Dependencies**: None required

## Installation

1. Stop your server.
2. Place the plugin `.jar` in `plugins/`.
3. Start server once to generate default files.
4. Edit:
   - `plugins/FaunaReborn/config.yml`
   - `plugins/FaunaReborn/entities/chicken.yml`
   - `plugins/FaunaReborn/entities/cow.yml`
   - `plugins/FaunaReborn/entities/pig.yml`
5. Run `/fauna reload` or restart.

## Quick Configuration Notes

- `global-enabled`: master switch.
- `world-filter`: global activation mode and world list.
- `targeting.scoring`: weighted target priority behavior.
- `activation.*`: natural-spawn and naming filters.
- Entity files (`entities/*.yml`): species-specific aggression, social and environmental behavior.

## Why Server Owners Choose FaunaReborn

- Keeps vanilla mobs recognizable while adding premium-tier encounter depth.
- Fully configurable for casual SMP, hardcore survival, or RPG progression.
- Safe defaults with explicit performance guardrails.
- Designed for production deployments and marketplace-grade delivery.

## Marketplace-Ready Summary

FaunaReborn is a configurable hostile-fauna combat plugin for Paper/Folia servers, featuring per-species AI modules, social propagation, weather/moon aggression scaling, GUI controls, and performance-focused safeguards.

## Support

For support, updates, and issue tracking, use your official distribution channel and repository release page.

## License

This project is licensed under the **MIT License**.
See [LICENSE](./LICENSE) for full details.
