---
name: bug-fix-testing
description: Patterns for creating tests that verify bug fixes and prevent regressions. Use when writing tests for bug fixes, creating regression tests, or verifying fix effectiveness.
---

# Bug Fix Testing

This skill provides testing patterns specifically designed for bug fix verification.

## When to Use

- Creating regression tests for a specific bug fix
- Writing tests that prove a bug is fixed
- Verifying fix effectiveness
- Adding edge case coverage related to a bug

## Test Types Required for Bug Fixes

### 1. Regression Test (Required)

Proves the specific bug is fixed and prevents recurrence:

```java
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TICKET-ID: Brief bug description")
class TicketIdRegressionTest {
    
    @Test
    @DisplayName("should not exhibit the bug behavior")
    void shouldNotExhibitBugBehavior() {
        // Arrange: Set up the exact condition that triggered the bug
        // Use steps from bug-context.md "Steps to Reproduce"
        
        // Act: Trigger the scenario that caused the bug
        
        // Assert: Verify correct behavior (not bug behavior)
    }
}
```

**Key Principle**: This test should FAIL on the old code and PASS with the fix.

### 2. Edge Case Tests (Recommended)

Cover related scenarios identified in the RCA:

```java
@Test
@DisplayName("should handle edge case from RCA")
void shouldHandleEdgeCase() {
    // Test boundary conditions and related scenarios
}
```

### 3. Integration Test (If Applicable)

Verify the fix works in realistic context:

```java
@Test
@DisplayName("should work correctly in real-world scenario")
void shouldWorkInRealWorldScenario() {
    // End-to-end verification with mocked dependencies
}
```

## Test Naming Convention

### File Naming
Format: `[TicketId][Component]Test.java`

**Examples:**
- `EMS1234UserAuthTest.java`
- `TMSPROV567DataSyncTest.java`

### Method Naming
Format: `shouldNotExhibit[BugBehavior]` or `should[ExpectedBehavior]When[Condition]`

**Examples:**
- `shouldNotReturnNullWhenFirmCacheEmpty()`
- `shouldFlushCacheAfterEntityUpdate()`
- `shouldHandleConcurrentCacheAccess()`

## Verification Approach

### Proving Test Effectiveness

Since we cannot automatically run tests on old code:

1. **With fix**: Run the test → Should PASS ✅
2. **Optional verification**: 
   - Temporarily revert the fix
   - Run the test → Should FAIL ❌
   - Re-apply the fix

### Test Quality Checklist

- [ ] Test uses exact scenario from bug report
- [ ] Test would fail without the fix
- [ ] Test is deterministic (no flakiness)
- [ ] Test is isolated (no external dependencies)
- [ ] Test has clear assertion messages
- [ ] Test covers the root cause, not just symptoms

## Java/JUnit 5 Templates

### Standard Regression Test

```java
package com.lucernex.rolloutmanager.businessobject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Regression tests for TICKET-ID: Bug title
 * Root Cause: [From verified-rca.md]
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TICKET-ID: Bug title")
class TicketIdRegressionTest {
    
    @Mock
    private FirmCache mockFirmCache;
    
    private BOServiceEntityRequest serviceRequest;
    
    @BeforeEach
    void setUp() {
        // Set up conditions that existed when bug occurred
        serviceRequest = new BOServiceEntityRequest();
    }
    
    @Test
    @DisplayName("should not exhibit bug behavior")
    void shouldNotExhibitBugBehavior() {
        // Arrange: Recreate the exact bug condition
        when(mockFirmCache.getEntity(anyInt())).thenReturn(null);
        
        // Act: Trigger the bug scenario
        var result = serviceRequest.processEntity(testData);
        
        // Assert: Verify correct behavior
        assertNotNull(result, "Should not return null");
        assertEquals(expectedValue, result.getValue());
    }
    
    @Test
    @DisplayName("should handle edge case from RCA")
    void shouldHandleEdgeCase() {
        // Test boundary conditions and related scenarios
    }
}
```

### Testing BOService Pattern

```java
package com.lucernex.rolloutmanager.businessobject;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for BOService business logic
 */
class BOServiceTest {
    
    @Test
    void shouldFlushCacheAfterUpdate() {
        // Given
        int firmId = 123;
        BOFlushCache.flushCache(firmId, "EntityType");
        
        // When
        var service = new BOServiceEntityRequest();
        service.updateEntity(testData);
        
        // Then
        verify(mockCache).flushCache(firmId, "EntityType");
    }
}
```

### Testing with Database Mocks

```java
@ExtendWith(MockitoExtension.class)
class DatabaseEntityTest {
    
    @Mock
    private EntityManager entityManager;
    
    @Test
    void shouldHandleNullQueryResult() {
        // Arrange
        when(entityManager.find(Entity.class, 1)).thenReturn(null);
        
        // Act & Assert
        assertDoesNotThrow(() -> service.findById(1));
    }
}
```

## Common Anti-Patterns to Avoid

### ❌ Testing the fix, not the bug
```java
// BAD: Tests that the fix exists
@Test
void shouldCallNewValidationMethod() {
    verify(validator).newValidationMethod();
}

// GOOD: Tests that the bug behavior doesn't occur
@Test
void shouldRejectInvalidInput() {
    assertThrows(ValidationException.class, 
        () -> service.process(invalidInput));
}
```

### ❌ Vague assertions
```java
// BAD
assertNotNull(result);

// GOOD
assertEquals("success", result.getStatus());
assertTrue(result.getErrors().isEmpty());
```

### ❌ Testing too much
```java
// BAD: Unrelated assertions in one test
@Test
void shouldFixTheBug() {
    // Tests 10 different behaviors - hard to maintain
}

// GOOD: Focused on specific behavior
@Test
void shouldNotAllowNullValues() {
    // Tests only null handling
}
```

### ❌ Not using Mockito for dependencies
```java
// BAD: Real dependencies make tests slow/flaky
@Test
void shouldQueryDatabase() {
    var realCache = new FirmCache(); // Real cache!
}

// GOOD: Mock dependencies
@Mock
private FirmCache mockFirmCache;

@Test
void shouldQueryDatabase() {
    when(mockFirmCache.getEntity(1)).thenReturn(testEntity);
}
```

## References

- Bug context: Use `bug-context.md` for exact reproduction steps
- Root cause: Use `verified-rca.md` for edge cases to cover
- Existing tests: Check `research/codebase-research.md` for test patterns in this repo
- Test locations:
  - `lx-common/src/test/java/` - Common utilities tests
  - `RolloutManager/junit/` - Business logic tests
- Test style guide: `.github/instructions/test-style.instructions.md`

## Running Tests

```powershell
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=TicketIdRegressionTest

# Run with verbose output
mvn test -Dtest=TicketIdRegressionTest -DfailIfNoTests=false
```

````
