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

## Release-Ready Track (`0.x`) vs Public Launch (`1.0.0`)

- Current strategy is **release-ready, not release-now**:
  - keep release automation and artifacts continuously healthy on `0.x`
  - continue feature development in parallel
- Use RC drill tags (`v0.x.y-rc.z`) to validate full release automation without committing to a public stable cut.
- Only cut `v1.0.0` after an explicit go-public decision and after release-readiness gates are green (docs, packaging, publishing, QA, platform validation).
- Before the final `v1.x` cut, remove the temporary macOS jpackage app-version major-floor workaround so demo and core versions match exactly again.

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
- Ensure [CHANGELOG.md](CHANGELOG.md) contains an up-to-date `Unreleased` section for the release scope.

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
- Release workflow runs as a three-stage pipeline:
  - `build-release-assets` (Linux): `clean test`, `:snapfx-core:jar`, `:snapfx-demo:distZip`, `:snapfx-demo:distTar`
  - `build-demo-jpackage` (matrix: Windows/macOS/Linux): `:snapfx-demo:jpackageImage` + `:snapfx-demo:packageJPackageImageZip`
  - `publish-release` (Linux): collects all artifacts, generates notes with `git-cliff`, and publishes one GitHub Release
- Demo `jpackage` assets are published with OS-specific names:
  - `snapfx-demo-jpackage-image-windows-<tag>.zip`
  - `snapfx-demo-jpackage-image-macos-<tag>.zip`
  - `snapfx-demo-jpackage-image-linux-<tag>.zip`

## Demo Smoke Validation (Per OS)

Use these commands after unpacking `snapfx-demo-jpackage-image-<os>-<tag>.zip`:

- Windows:
  - `.\SnapFX-Demo\bin\SnapFX-Demo.exe`
- macOS:
  - `open ./SnapFX-Demo.app`
- Linux:
  - `chmod +x ./SnapFX-Demo/bin/SnapFX-Demo`
  - `./SnapFX-Demo/bin/SnapFX-Demo`

Smoke policy:
- Required: validate the package on at least one local/developer OS environment per release candidate.
- Nice to have: validate additional OS packages manually when hardware/VMs are available.
- Covered by CI: release pipeline already builds/packages all OS artifacts; manual smoke exists to detect runtime UX regressions not covered by build success alone.

Minimum smoke checklist for the tested OS package(s):
- App starts without local JDK installation.
- Main window opens with expected default layout.
- One basic interaction works (for example moving a panel tab and closing/reopening a node).
- App exits cleanly without crash dialog.

### Optional CI Startup Smoke (Recommended)

Feasible CI automation scope:
- Start the packaged demo executable/app image per OS in a short timeout window.
- Verify process startup and non-crash exit path (or controlled termination after successful start probe).

Out of scope for pure CI startup smoke:
- Rich GUI interaction checks like drag-and-drop UX parity across OS (these remain manual or require dedicated UI automation infrastructure).

## Release Notes

- Release notes are generated with `git-cliff` (`cliff.toml`).
- Notes are grouped by commit prefix between tags.
- Keep [CHANGELOG.md](CHANGELOG.md) aligned with tagged releases by moving `Unreleased` entries into the new tag section.
