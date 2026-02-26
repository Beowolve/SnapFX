# SnapFX Testing Policy

**Last Updated**: 2026-02-16

This document defines stable testing rules only.  
Current test counts, trends, and recent bug history belong in [STATUS.md](STATUS.md) and [DONE.md](DONE.md).

## Scope

- Applies to all contributors and all changes in this repository.
- Covers model, view, persistence, drag-and-drop, floating windows, and demo wiring.
- Covers both automated tests and required manual verification for UI behaviors that are not reliably automatable.

## Mandatory Rules

### 1. Bug Fix Rule (Regression Required)

- Every bug fix must include at least one regression test.
- The regression test must fail before the fix and pass after the fix.
- The test should clearly describe the prevented failure scenario.

### 2. Feature Rule (Coverage Required)

- Every new feature must include automated test coverage.
- Minimum expectation per feature: happy-path behavior, edge/boundary behavior, invalid-input or error-path behavior (if applicable), and integration with affected existing behavior.

### 3. Refactor Rule (Safety Required)

- Refactors must keep existing tests green.
- If behavior changes, tests must be updated intentionally and explicitly.
- No speculative refactor merge without executable proof.

### 4. DnD Rule (Model Coverage Required)

- Any drag-and-drop change must include model-level tests for the affected behavior.
- If UI-level drag behavior cannot be automated reliably, manual verification steps must be documented in the related change notes.

### 5. Test Quality Rule

- Tests must be deterministic.
- Tests must be isolated and independent.
- Test names must describe behavior, not implementation detail.
- Assertions must make failures diagnosable.
- `@Disabled` tests require an issue reference and explicit reason.

### 6. Merge Gate Rule

- `./gradlew test` must pass before merge.
- No merge with failing tests.
- No merge that removes meaningful coverage without explicit team agreement.

## Quality Gates

- Core model behavior should maintain high coverage (target 90%+).
- View/UI logic should maintain practical coverage with automated and manual checks combined.
- Critical workflows (dock, move, undock, save/load, float/attach, close handling) must remain covered by tests.

## Manual Verification (When Needed)

Use manual checks only when UI automation is not reliable.  
Manual steps must be concrete, reproducible, and listed in the related change documentation.

Minimum manual checks for UI-sensitive changes:

- Drag preview visibility and target clarity
- Tab overflow/menu behavior
- Close/float/attach control behavior
- Lock-mode behavior parity

## Test Commands

```bash
./gradlew test
./gradlew test --tests "DockGraphTest"
./gradlew test --tests "DockGraphTest.testNoEmptyContainersAfterUndock"
./gradlew test jacocoTestReport
```

## Pull Request Checklist

- [ ] New behavior has automated tests.
- [ ] Bug fixes have regression tests.
- [ ] Drag-and-drop changes include model-level tests.
- [ ] Manual verification steps are documented when UI automation is not feasible.
- [ ] `./gradlew test` passes locally.

## Ownership

- Rule changes in this document require team agreement.
- Keep this file stable and policy-focused; move temporal project status to [STATUS.md](STATUS.md) and [DONE.md](DONE.md).
