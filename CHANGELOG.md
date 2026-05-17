# Changelog

All notable changes to this project will be documented in this file.

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
