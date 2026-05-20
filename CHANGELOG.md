# Changelog

All notable changes to this project will be documented in this file.

## [1.1.3] - 2026-05-20

### Fixed
- Restored cross-plugin compatibility for targeting behavior, including vanish and god-mode integrations.
- Hardened startup and shutdown cleanup paths to behave safely under partial startup failures.
- Cleaned up static-analysis findings across command, hostility, and lifecycle codepaths while preserving behavior.

### Changed
- Promoted project versioning from `1.1.2` to release `1.1.3` in Gradle project metadata.
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
