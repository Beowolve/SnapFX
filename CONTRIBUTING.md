# Contributing to SnapFX

Thanks for contributing to SnapFX.

## Scope

This guide is for contributors working on framework code, tests, documentation, and demo updates.

## Prerequisites

- Follow environment setup in `SETUP.md`.
- Use Java 21 and JavaFX 21.

## Branch Strategy

- Current phase (base implementation): work directly on `main`.
- Later phase (after `develop` exists): integrate feature work into `develop`.
- Release tags are created from `main` after merge.

## Development Workflow

1. Keep changes focused on one logical topic.
2. Add or update tests for every behavior change.
3. Run tests locally:
   - `./gradlew test`
4. Update documentation affected by the change.
5. Keep documentation ownership clear:
   - `STATUS.md` = current state
   - `ROADMAP.md` = planned work
   - `DONE.md` = completed work
   - `TESTING_POLICY.md` = stable testing rules

## Commit Conventions

- Use focused commits (avoid mixed unrelated changes).
- Use git-cliff-compatible prefixes:
  - `feat:`, `fix:`, `docs:`, `test:`, `refactor:`, `perf:`, `ci:`, `build:`, `chore:`
  - Domain-specific prefixes are also allowed when they map to release-note groups.
- For fix commits, use a multi-line body with at least one explanation line per fix.

## Pull Request Checklist

- [ ] Scope is focused and coherent.
- [ ] Tests are added/updated as needed.
- [ ] `./gradlew test` passes.
- [ ] `STATUS.md`, `DONE.md`, and `ROADMAP.md` are updated if required.
- [ ] Other affected docs are updated (`README.md`, `SETUP.md`, etc.).
- [ ] MainDemo visual changes include updated preview image (`docs/images/main-demo.png`) regenerated with `./scripts/update-main-demo-preview.ps1`.

## Collaboration Rules

- For AI-assisted contributions, persistent collaboration rules are defined in `AGENTS.md`.
- Use minimal targeted diffs and avoid full-file rewrites when smaller edits are sufficient.
