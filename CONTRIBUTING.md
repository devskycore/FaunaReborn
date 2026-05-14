# Contributing to FaunaReborn

Thanks for your interest in improving FaunaReborn.
This project accepts contributions, but reviews are intentionally strict to protect gameplay quality, server stability, and production safety.

## Maintainer Review Policy

All pull requests and issues are reviewed in detail by the project creator/maintainer.

What this means in practice:
- Every PR is manually reviewed for behavior, architecture, performance impact, and config compatibility.
- Every issue is triaged carefully; incomplete reports may be asked for more details before action.
- Fast merge is not guaranteed. Quality and long-term maintainability have priority.

## Before You Start

- Search existing issues/PRs first to avoid duplicates.
- Open an issue before implementing large changes.
- Keep scope focused: one feature/fix per PR.

## Development Setup

### Requirements
- JDK 21
- Gradle wrapper (included)

### Build
```bash
./gradlew clean build
```
On Windows PowerShell:
```powershell
.\gradlew.bat clean build
```

## Branching and Commit Guidelines

- Branch from `main`.
- Use descriptive branch names:
  - `feat/<short-topic>`
  - `fix/<short-topic>`
  - `refactor/<short-topic>`
  - `docs/<short-topic>`
- Write clear commits in imperative style.
  - Good: `Improve chicken target scoring cooldown handling`
  - Avoid: `changes`, `fix stuff`

## Coding Expectations

Contributions should match the existing project style and architecture:
- Keep modules and responsibilities well separated.
- Avoid unnecessary abstraction and avoid dead code.
- Preserve Paper/Folia compatibility.
- Preserve configuration behavior unless the PR explicitly proposes a breaking change.
- Consider performance (tick-time, loops, allocations, scheduler usage).

## Testing and Verification

Before opening a PR, please:
- Build successfully with Gradle.
- Verify no obvious warnings/errors are introduced.
- Smoke-test behavior on a Paper server when relevant.
- Include reproducible steps for bug fixes.

If your change affects gameplay logic, include:
- What was happening before.
- What should happen now.
- How you validated the result.

## Pull Request Checklist

Please make sure your PR includes:
- Clear description of the problem and solution.
- Scope and rationale (why this approach).
- Any config changes and migration notes.
- Test/validation notes.
- Linked issue (if applicable).

PRs may be closed if they are:
- Too broad or unrelated in scope.
- Missing validation details.
- Risky for performance/stability without strong justification.
- Not aligned with project direction.

## Reporting Issues

When opening an issue, include as much detail as possible:
- FaunaReborn version.
- Server software and version (Paper/Folia build).
- Minecraft version.
- Steps to reproduce.
- Expected behavior vs actual behavior.
- Relevant logs and config snippets.

Bug reports without reproduction steps may be delayed until more information is provided.

## Security

Do not disclose vulnerabilities publicly first.
If you discover a security issue, report it privately to the maintainer.
(Recommended: add a `SECURITY.md` with a dedicated contact channel.)

## License

By contributing, you agree that your contributions are licensed under the same MIT License used by this repository.
