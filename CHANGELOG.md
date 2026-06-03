# Changelog

All notable changes to this project will be documented in this file.

## [1.3.0] - 2026-06-03

### Added
- Added premium page-navigation feedback for `/fauna help`: subtle sound cue for players only when changing pages via `PREV/NEXT`.
- Added explicit fallback guidance for unknown commands when no close match exists (`Try: /fauna help`), with suggest-click support.

### Changed
- Upgraded command output UX to a unified visual block style across `help`, `about`, `version`, and `entities`:
  - Consistent header style (`FaunaReborn · <TITLE>`), spacing, bullets, and color hierarchy.
  - Consistent line formatting for command/metadata output.
- Improved help interaction behavior:
  - Safe commands now run directly on click; sensitive commands use suggest-on-click.
  - Hover hints now reflect real action (`Run`/`Suggest`) with visual emphasis.
  - Console navigation now shows actionable plain commands instead of click-oriented row styling.
- Refined reload status presentation:
  - Clearer severity coloring (`already in progress` as warning/error style).
  - Cleaner typography (icon emphasis only, no unnecessary bold on body text).
- Localized block titles and about-section labels (`header-label`, `tagline-label`, `summary-label`, `details-label`) so titles/labels are language-correct instead of forced English.
- Normalized command terminology and readability in EN/ES/PT/FR/IT translations, including more natural short-form phrasing.
- Synced public command and locale documentation with current plugin behavior:
  - Documented built-in locale support plus custom locale-file behavior.
  - Documented `/fauna lang` selector/runtime switching behavior and accepted aliases.
  - Documented `/fauna help` page/admin/permissions/query modes and all command aliases.

### Fixed
- Restored explicit optional vs required argument emphasis in help usage rendering (`[optional]` vs `<required>`).
- Fixed multiple encoding/mojibake regressions in symbols and separators used by command output rendering.

## [1.2.0] - 2026-05-21

### Fixed
- Restored cross-plugin compatibility for targeting behavior, including vanish and god-mode integrations.
- Hardened startup and shutdown cleanup paths to behave safely under partial startup failures.
- Cleaned up static-analysis findings across command, hostility, and lifecycle codepaths while preserving behavior.

### Changed
- Promoted project versioning from `1.1.2` to release `1.2.0` in Gradle project metadata.
- Updated migration backup filename tagging to use the runtime plugin version instead of a stale hardcoded release tag.

## [1.1.2] - 2026-05-17

### Fixed
- Fixed multiple Folia thread-safety issues that caused `Thread failed main thread check` errors in chicken hostility flows.
- Fixed chicken tracking/reload startup paths to avoid off-thread entity access during module enable and GUI reload.
- Fixed chicken social alert recruitment flow to run recruitment logic on entity-owned schedulers in Folia.
- Fixed chicken visual cleanup on reload/shutdown to restore glow state safely without cross-thread entity access.
- Fixed shutdown-time `IllegalPluginAccessException` (`Plugin attempted to register task while disabled`) during chicken module disable on Folia.
- Fixed cow melee aggression cooldown gating bug that could prevent cows from dealing damage.
- Fixed pig direct retaliation behavior on damage so the victim pig now responds consistently and can trigger herd response.
- Fixed Folia pig processing tick guard that could skip aggression processing.
- Fixed custom hostility death messages not applying consistently on Folia by prioritizing tracked hostility context over last-damage fallback.

### Stability
- Improved Folia stability across startup, hot reload (`/fr gui`), runtime hostility processing, and shutdown paths.
