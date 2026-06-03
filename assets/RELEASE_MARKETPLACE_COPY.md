# FaunaReborn Release / Marketplace Copy Draft

## Release Notes Copy

### Commands + Locale UX Sync
This release aligns all public-facing command and locale documentation with the plugin's live behavior.

- Documented every active command path for `/fauna help`:
  - `/fauna help [page]`
  - `/fauna help admin [page]`
  - `/fauna help permissions [page]`
  - `/fauna help <query>`
- Documented all active command aliases:
  - Root: `/faunareborn`, `/fr`
  - Subcommands: `h`, `?`, `ver`, `v`, `info`, `ent`, `entity`, `rl`, `menu`, `language`
- Clarified `/fauna lang` behavior:
  - Players with permission open the language selector via `/fauna lang`
  - Console usage prints command usage and available locales
  - Runtime switching supports locale codes, common aliases, and custom locale file names
- Clarified supported built-in locales:
  - `en`, `es`, `pt`, `it`, `fr` (mapped to `english.yml`, `spanish.yml`, `portuguese.yml`, `italian.yml`, `french.yml`)
- Clarified custom locale behavior:
  - Any additional `plugins/FaunaReborn/lang/*.yml` file can be selected by its filename base

### Help UX Notes
- Help docs now match real in-game behavior: pagination, admin filtering, permissions mode, and query search.
- Alias behavior and clickable command guidance are now documented consistently across plugin metadata and README.

## Marketplace Long Description Add-on

FaunaReborn includes a production-ready command and locale workflow built for server administrators:

- Complete `/fauna help` navigation with paginated, admin-only, permissions, and search modes.
- Runtime language switching with `/fauna lang` and selector GUI integration.
- Built-in locales (`en`, `es`, `pt`, `it`, `fr`) plus support for custom locale files.
- Full alias coverage for fast command access (`/fr`, `/fauna ?`, `/fauna ver`, `/fauna language`, and more).

## Marketplace Short Description Options

1. "Hostile-fauna AI plugin for Paper/Folia with advanced `/fauna` admin UX, runtime locale switching, and built-in plus custom locale support."
2. "Turn passive mobs hostile with premium AI, command-first admin control, and live multi-locale support for Paper/Folia servers."
3. "Configurable hostile-animal encounters for Paper/Folia with advanced help UX, GUI controls, and runtime language switching."

