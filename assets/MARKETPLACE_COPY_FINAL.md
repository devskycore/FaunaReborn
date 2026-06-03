# FaunaReborn Marketplace Copy (Final)

## Canonical Release Title
FaunaReborn 1.3.0 - Command & Locale UX Sync

## Canonical Release Notes
This release aligns all public-facing command and locale documentation with the plugin's current runtime behavior.

### What changed
- Synced `/fauna help` docs with live behavior:
  - `/fauna help [page]`
  - `/fauna help admin [page]`
  - `/fauna help permissions [page]`
  - `/fauna help <query>`
- Synced `/fauna lang` docs with live behavior:
  - `/fauna lang` opens the language selector for players
  - Console usage shows usage + available locales
  - Runtime switching accepts locale codes, common aliases, and custom locale file names
- Synced documented aliases with active command aliases:
  - Root: `/faunareborn`, `/fr`
  - Subcommands: `h`, `?`, `ver`, `v`, `info`, `ent`, `entity`, `rl`, `menu`, `language`
- Synced supported built-in locales:
  - `en`, `es`, `pt`, `it`, `fr`
- Clarified custom locale support:
  - Any extra `plugins/FaunaReborn/lang/*.yml` file can be selected by filename base

---

## Spigot

### Short Description
Hostile-fauna AI for Paper/Folia with advanced `/fauna` help UX, runtime locale switching, and production-safe performance controls.

### Long Description
FaunaReborn transforms passive farm mobs into dynamic hostile encounters while keeping vanilla identity and server stability in mind.

Key highlights:
- Chicken, cow, and pig hostility modules with configurable behavior.
- Social alert propagation and environment-based aggression scaling.
- GUI controls and hot-reload workflow for admin operations.
- Advanced command UX:
  - Paginated/searchable `/fauna help`
  - Admin and permissions help modes
  - Full command alias coverage
- Runtime locale workflow:
  - Built-in locales: `en`, `es`, `pt`, `it`, `fr`
  - `/fauna lang` selector and runtime switch
  - Custom locale files supported (`lang/*.yml`)

Built for production:
- LOD processing tiers
- World filtering
- Tick/chunk/world safety caps
- Paper/Folia-ready architecture

### Spigot Update/Version Message
`1.3.0` syncs command + locale documentation with real runtime behavior, including `/fauna help` modes, `/fauna lang` behavior, and full alias coverage.

---

## Hangar

### Summary
Configurable hostile-fauna plugin for Paper/Folia with advanced command UX, GUI controls, and runtime locale switching.

### Description
FaunaReborn adds intelligent hostile behavior to chickens, cows, and pigs with a strong focus on configurability, admin usability, and stable long-term server operation.

This release improves publication consistency by aligning all public command and locale docs with actual plugin behavior:
- Accurate `/fauna help` mode coverage
- Accurate `/fauna lang` runtime behavior and accepted locale inputs
- Accurate alias coverage for root and subcommands
- Accurate built-in locale + custom locale file support

If you're operating multilingual communities, this release makes command-language expectations explicit and predictable for both players and staff.

### Release Notes Block
Command and locale UX docs are now fully synchronized with live behavior (`/fauna help`, `/fauna lang`, aliases, built-in + custom locales).

---

## Modrinth

### Project Summary
Hostile-fauna AI for Paper/Folia with advanced `/fauna` commands, runtime language switching, and server-safe performance controls.

### Project Description
FaunaReborn turns passive farm mobs into configurable hostile encounters using species-specific modules, social aggression propagation, and environment-aware behavior scaling.

Includes:
- Chicken/Cow/Pig hostility modules
- Admin GUI + reload workflow
- `/fauna help` pagination, admin view, permissions view, and query search
- `/fauna lang` selector + runtime locale switching
- Built-in locale support (`en`, `es`, `pt`, `it`, `fr`) and custom `lang/*.yml` locales
- Performance guardrails for real server workloads

### Version Changelog (Modrinth)
- Synced command docs with live `/fauna help` behavior and modes.
- Synced locale docs with live `/fauna lang` behavior and accepted aliases.
- Synced documented command aliases with active plugin aliases.
- Clarified built-in and custom locale support.

---

## Optional Spanish Variant (if needed for announcements)

### Texto breve
Plugin de fauna hostil para Paper/Folia con UX avanzada de `/fauna`, cambio de idioma en runtime y controles de rendimiento para producción.

### Texto de actualización
La versión `1.3.0` alinea toda la documentación pública de comandos e idiomas con el comportamiento real del plugin: modos de `/fauna help`, flujo real de `/fauna lang`, aliases activos y soporte de locales (incluyendo archivos personalizados).

