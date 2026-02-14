# SnapFX Testing Policy

**Established**: 2026-02-10  
**Mandatory for**: All contributors  
**Goal**: Extreme stability of the docking framework

## Core Principle

> **Every fixed bug MUST have a regression test.**  
> **Every new feature MUST have test coverage.**

This ensures that fixed bugs never reappear and the framework remains rock-solid through future changes.

---

## Testing Rules

### Rule 1: Regression Tests for Bug Fixes (MANDATORY)

When a bug is fixed:

1. ✅ **Create a test** that reproduces the bug
2. ✅ **Verify test fails** before the fix
3. ✅ **Verify test passes** after the fix
4. ✅ **Document** the bug in test comments with:
   - Bug description
   - Date fixed
   - What was wrong
   - What was fixed

**Example:**
```java
/**
 * Test for empty container prevention bug fix.
 * Regression test: Ensures no empty SplitPanes remain after D&D operations.
 * Bug: After removing children, empty containers stayed in tree.
 * Fix: Reordered cleanup logic - flatten first, then cleanup.
 * Date: 2026-02-10
 */
@Test
void testNoEmptyContainersAfterUndock() {
    // ... test code
}
```

### Rule 2: Feature Tests (MANDATORY)

When a new feature is implemented:

1. ✅ **Test happy path** - feature works as expected
2. ✅ **Test edge cases** - boundary conditions
3. ✅ **Test error cases** - invalid inputs
4. ✅ **Test integration** - feature works with existing code

### Rule 3: Test Coverage Goals

- **Core Model**: 90%+ coverage
- **View Layer**: 75%+ coverage
- **Integration**: Critical paths 100% covered

### Rule 4: Test Quality Standards

All tests must:

- ✅ **Be deterministic** - same input = same output
- ✅ **Be isolated** - no dependencies between tests
- ✅ **Be fast** - unit tests < 100ms each
- ✅ **Have clear names** - describe what they test
- ✅ **Have clear assertions** - fail messages explain what went wrong

### Rule 5: Continuous Testing

- ✅ **Run tests before commit**
- ✅ **All tests must pass** before merging
- ✅ **No commented-out tests** (fix or delete)
- ✅ **No @Disabled tests** without issue reference

---

## Test Categories

### Unit Tests (`*Test.java`)

Test individual classes in isolation:

- ✅ `DockGraphTest` - Model logic
- ✅ `DockNodeTest` - Node behavior
- ✅ `DockLayoutSerializerTest` - Persistence
- ✅ `DockLayoutEngineTest` - View creation

**Location**: `src/test/java/`

### Integration Tests

Test interactions between components:

- ✅ D&D sequences
- ✅ Layout persistence round-trips
- ✅ View synchronization with model

### UI Tests (TestFX)

Test visual components and user interactions:

- ✅ `DockLayoutEngineTest` - Uses TestFX
- ⚠️ More UI tests needed

### Manual UI Checks

Use these checks when behavior cannot be reliably automated:

- Drag a DockNode: the ghost overlay appears offset bottom-right of the cursor and does not cover the drop target.
- Shrink a TabPane until the overflow dropdown appears; open it and confirm the entries show tab titles.
- Confirm the tab close button is visible without hover and the tab/title close buttons share the same hover color change.
- Open the Settings tab in the debug panel; change title bar mode, close button mode, drop visualization, and lock state to confirm immediate updates.

---

## Critical Bug Regression Tests

These tests ensure fixed critical bugs never return:

### 2026-02-10: Empty Container Prevention
- `testNoEmptyContainersAfterUndock()`
- `testTabPaneFlattensWhenOnlyOneTabRemains()`
- Helper: `assertNoEmptyContainers()`

**Bug**: After undocking nodes, empty SplitPanes/TabPanes remained in tree  
**Fix**: Reordered cleanup logic - flatten first, then cleanup

### 2026-02-10: Target Invalidation During Move
- `testTargetInvalidationDuringMove()`
- `testComplexDndSequence_BottomThenLeft()`

**Bug**: During move(), undock() caused flattening → target reference became invalid → empty containers  
**Fix**: Find target by ID after undock to handle tree restructuring

### 2026-02-10: TabPane D&D Cache Bug
- Covered by `DockLayoutEngineTest`

**Bug**: Tabs lost D&D capability after being moved to TabPane  
**Fix**: Clear view cache when rebuilding TabPane

### 2026-02-10: Auto-Rebuild After D&D
- Implicitly covered by all D&D tests

**Bug**: D&D stopped working after first drop  
**Fix**: Auto-rebuild on revision change

### 2026-02-10: Locked State Synchronization
- `testLockedStateRoundTrip()`

**Bug**: UI locked property not synchronized after loading layout  
**Fix**: Sync lockLayoutProperty with DockGraph.isLocked() after load

### 2026-02-14: No-Op Drop Preserves Divider Positions
- `testMoveToParentSplitEdgeIsNoOp()`

**Bug**: Dropping on the parent split edge still adjusted dividers when no layout change occurred  
**Fix**: Detect no-op edge drops on the parent SplitPane and skip the move

### 2026-02-14: TabPane In-Place Reordering
- `testMoveWithinSameTabPaneDoesNotFlatten()`
- `testMoveWithinSameTabPaneAdjustsInsertIndex()`

**Bug**: Moving a tab within the same TabPane could flatten the TabPane or misplace the insert index  
**Fix**: Reorder within the TabPane without undocking and adjust forward insert indices

### 2026-02-14: Tab Close Handler Consistency
- `testTabCloseRequestUsesCloseHandler()`

**Bug**: Tab close bypassed onNodeCloseRequest, so hidden nodes were not tracked  
**Fix**: Route tab close requests through the shared close handler

### Structural Integrity Tests
- `assertNoEmptyContainers()` - No empty containers anywhere
- `assertNoNestedTabPanes()` - No TabPane in TabPane
- `assertNoNestedSplitPanesWithSameOrientation()` - No unnecessary nesting

---

## Test Development Workflow

### 1. Bug Reported
```
1. Reproduce bug manually
2. Export DockGraph snapshot
3. Create failing test from snapshot
4. Fix bug
5. Verify test passes
6. Document in test comments
```

### 2. Feature Request
```
1. Write test for expected behavior (TDD)
2. Test fails (red)
3. Implement feature
4. Test passes (green)
5. Refactor if needed
6. All tests still pass
```

### 3. Refactoring
```
1. All tests pass before refactoring
2. Refactor code
3. All tests still pass
4. No test changes needed (if tests are good)
```

---

## Running Tests

### All Tests
```bash
./gradlew test
```

### Specific Test Class
```bash
./gradlew test --tests "DockGraphTest"
```

### Specific Test Method
```bash
./gradlew test --tests "DockGraphTest.testNoEmptyContainersAfterUndock"
```

### With Coverage
```bash
./gradlew test jacocoTestReport
# Report: build/reports/jacoco/test/html/index.html
```

### Continuous Testing (Watch Mode)
```bash
./gradlew test --continuous
```

---

## Test Naming Conventions

### Unit Tests
- `test<MethodName>()` - Tests a specific method
- `test<MethodName>_<Scenario>()` - Tests a method in specific scenario
- `test<Feature>()` - Tests a feature or behavior

### Regression Tests
- `test<BugDescription>()` - Clear description of what bug is prevented
- Must include JavaDoc with bug details and fix date

### Integration Tests
- `test<Workflow>()` - Tests a complete workflow
- `test<Component1>With<Component2>()` - Tests interaction

---

## Test Documentation

Each test should have:

```java
/**
 * [One-line summary of what is tested]
 * [Optional: Detailed description]
 * [Optional: Regression test info - bug, date, fix]
 */
@Test
void testSomething() {
    // Arrange (Given)
    // ... setup
    
    // Act (When)
    // ... execute
    
    // Assert (Then)
    // ... verify
}
```

---

## Current Test Statistics

**As of 2026-02-14:**

- **Total Tests**: 82 (was 49)
- **Test Classes**: 4
- **Test Coverage**: ~87% (estimated)
- **All Tests**: ✅ PASSING

### Test Distribution
- `DockGraphTest`: 51 tests (was 27)
  - 11 new regression tests added
- `DockLayoutSerializerTest`: 9 tests (was 8)
  - 1 new regression test added
- `DockLayoutEngineTest`: 15 tests
- `SnapFXTest`: 7 tests

### Regression Test Coverage
- ✅ Empty container prevention
- ✅ Target invalidation during move
- ✅ Complex D&D sequences
- ✅ TabPane flattening
- ✅ SplitPane flattening
- ✅ Cache invalidation
- ✅ Auto-rebuild
- ✅ Locked state synchronization
- ✅ No-op drop divider preservation
- ✅ TabPane in-place reordering
- ✅ Tab close handler consistency
- ✅ SplitPane middle insert divider preservation
- ✅ View cache cleanup and boundedness across rebuild cycles
- ✅ Null/no-op/detached-target edge case handling

---

## Benefits of This Policy

### For Developers
- 🎯 **Confidence** - Changes won't break existing functionality
- 🚀 **Speed** - Fast feedback on code changes
- 📚 **Documentation** - Tests document expected behavior
- 🔍 **Debugging** - Failing test pinpoints exact problem

### For Users
- ✅ **Stability** - Bugs stay fixed
- ✅ **Quality** - High code quality
- ✅ **Trust** - Professional framework
- ✅ **Predictability** - Consistent behavior

### For Project
- 📈 **Maintainability** - Easy to refactor with safety net
- 🛡️ **Regression Prevention** - Fixed bugs never return
- 🎓 **Knowledge Base** - Tests preserve understanding
- 🏆 **Professional** - Industry best practices

---

## Enforcement

### Pre-Merge Checklist

Before merging any PR:

- [ ] All existing tests pass
- [ ] New tests added for changes
- [ ] Regression tests for bug fixes
- [ ] Test coverage maintained or improved
- [ ] No @Disabled tests
- [ ] Tests are documented

### Automated Checks

- ✅ CI/CD runs all tests on every commit
- ✅ Coverage reports generated
- ✅ Failed tests block merge
- ✅ Coverage decrease warns reviewer

---

## Future Improvements

### Planned
- 📋 Automated mutation testing
- 📋 Property-based testing for model
- 📋 Performance benchmarks
- 📋 Performance trend tracking for large-layout stress tests
- 📋 Automated heap profiling and leak trend reporting
- 📋 Multi-threaded operation tests

### Nice to Have
- 💡 Visual regression tests (screenshot comparison)
- 💡 Automated test generation from bugs
- 💡 Test coverage dashboard
- 💡 Test execution time tracking

---

## Resources

### JUnit 5
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Assertions](https://junit.org/junit5/docs/current/api/org.junit.jupiter.api/org/junit/jupiter/api/Assertions.html)

### TestFX
- [TestFX Documentation](https://github.com/TestFX/TestFX)
- [JavaFX Testing](https://github.com/TestFX/TestFX/wiki)

### Best Practices
- [Test-Driven Development](https://martinfowler.com/bliki/TestDrivenDevelopment.html)
- [Testing Strategies](https://martinfowler.com/testing/)

---

## Questions?

If you're unsure:
1. Look at existing tests as examples
2. Ask the team
3. When in doubt, write the test!

**Better to have too many tests than too few.**

---

**Remember**: Tests are not optional. They are part of the implementation.

**Version**: 1.0  
**Last Updated**: 2026-02-14  
**Status**: Active and Enforced

