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
- Prefer `getFirst()`, `getLast()`, and `isEmpty()` over direct index access when available.
