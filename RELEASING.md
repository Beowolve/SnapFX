# SnapFX Releasing Guide

This guide describes the maintainer release flow for SnapFX.

## Versioning Model

- Versioning is Git-driven via `gradle-jgitver` (configured in `build.gradle.kts`).
- Release tags are the source of truth and must follow `vX.Y.Z`.
- Without matching tags in local history, builds derive from `0.0.0`.

Inspect the resolved version:

```bash
./gradlew version
```

## Branch and Release Flow

- Current phase: active implementation happens on `main`.
- Future phase (after `develop` exists): integrate features on `develop`, then merge `develop` -> `main` for releases.
- Create release tags from `main`.

## Pre-Release Checklist

- Ensure local branch is up to date.
- Ensure working tree is clean.
- Run full test suite:
  - `./gradlew test`
- Verify documentation updates for user-visible changes.

## Tag and Publish

Create and push release tag:

```bash
git tag vX.Y.Z
git push origin main --tags
```

Milestone helper script:

```bash
./scripts/tag-roadmap-milestone.ps1 -Milestone "0.2"
```

Optional push using helper:

```bash
./scripts/tag-roadmap-milestone.ps1 -Milestone "0.2" -Push
```

## CI/CD Release Automation

- CI workflow (`.github/workflows/ci.yml`) runs tests on every push and pull request.
- Release workflow (`.github/workflows/release.yml`) runs when a `v*` tag is pushed.
- Release workflow builds and tests, then publishes GitHub Release artifacts.

## Release Notes

- Release notes are generated with `git-cliff` (`cliff.toml`).
- Notes are grouped by commit prefix between tags.
