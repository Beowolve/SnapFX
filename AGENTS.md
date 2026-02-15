# AI Agent Definitions

These instructions are shared for all AI agents working on this workspace.

## Core Workflow
- Always add or update unit tests for new behavior, bug fixes, or refactors.
- Always run unit tests after code changes when possible (`./gradlew test`).
- Always update `STATUS.md`, `DONE.md`, and `ROADMAP.md` after changes.
- Update other `*.md` files if the change affects their content or accuracy.
- Keep changes scoped and avoid unrelated edits.
- After successful changes, prepare a small, focused commit (stage relevant files and propose a commit message).
- Always `git add` new files when preparing a commit.
- Before creating a commit, always show the proposed commit message to the user and get confirmation.
- Keep commits minimally mixed: one logical topic/fix per commit; avoid bundling unrelated changes.
- If a fixed collaboration rule is agreed with the user, add it to this `AGENTS.md` immediately.
- Treat `AGENTS.md` as the source of truth for all persistent collaboration rules in this workspace.
- As soon as a focused, sensible commit is ready, proactively show the proposed commit message without waiting for the user to ask.
- If `MainDemo` visuals change (layout, window composition, look and feel, styling, icons, controls), always run `./scripts/update-main-demo-preview.ps1` and include the updated `docs/images/main-demo.png`.

## DnD-Specific Rules
- Any drag-and-drop change must include model-level tests for the affected behavior.
- If UI-level behavior cannot be tested automatically, document manual verification steps.

## Coding Standards
- Follow existing code style and conventions.
- Use descriptive variable and method names.
- Add JavaDoc comments for new public methods and classes.
- Avoid introducing new dependencies unless necessary.
- Write all code in Java 21 and ensure compatibility with JavaFX 21.
- Write all code and documentation in English.
- Prefer explicit imports over fully-qualified JavaFX class names; use fully-qualified names only when unavoidable.
- Prefer `getFirst()`, `getLast()`, and `isEmpty()` over direct index access in production code when available; tests may use index access for clarity.
- Use `Math.clamp(...)` for range clamping instead of nested `Math.min(...)` and `Math.max(...)`.
- Keep function complexity at a reasonable level, aligned with SonarQube guidance (target low cognitive complexity; refactor methods approaching high complexity, e.g. around 15+ cognitive complexity).
