````markdown
# Bug Fix Test Template

Use this template when creating regression tests for bug fixes in the LX-IWMS codebase.

## Test File Location

| Module | Test Directory |
|--------|---------------|
| `lx-common` | `lx-common/src/test/java/com/lucernex/common/` |
| `RolloutManager` | `RolloutManager/junit/com/lucernex/rolloutmanager/` |

## File Naming

Format: `[TicketId][Component]Test.java`

**Examples:**
- `EMS1234UserAuthTest.java`
- `TMSPROV567DataSyncTest.java`

## Template

```java
package com.lucernex.rolloutmanager.businessobject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Regression tests for: [TICKET-ID]
 * Bug: [Brief description]
 * Root Cause: [From verified-rca.md]
 * 
 * These tests ensure the bug does not recur.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("[TICKET-ID]: [Bug title]")
class TicketIdComponentTest {
    
    // ============================================
    // MOCKS
    // ============================================
    
    @Mock
    private FirmCache mockFirmCache;
    
    @Mock
    private EntityManager mockEntityManager;
    
    // ============================================
    // SYSTEM UNDER TEST
    // ============================================
    
    private BOServiceEntityRequest serviceUnderTest;
    
    // ============================================
    // SETUP / TEARDOWN
    // ============================================
    
    @BeforeEach
    void setUp() {
        // Set up conditions that existed when bug occurred
        serviceUnderTest = new BOServiceEntityRequest();
    }
    
    @AfterEach
    void tearDown() {
        // Clean up - clear caches, reset state
        ThreadCache.getInstance().clearCache();
    }

    // ============================================
    // REGRESSION TEST (Required)
    // ============================================
    
    @Test
    @DisplayName("should not exhibit bug behavior")
    void shouldNotExhibitBugBehavior() {
        // Arrange: Recreate the exact bug condition
        // From bug-context.md "Steps to Reproduce"
        when(mockFirmCache.getEntity(anyInt())).thenReturn(null);
        
        // Act: Trigger the bug scenario
        var result = serviceUnderTest.processEntity(testData);
        
        // Assert: Verify correct behavior
        // This assertion should FAIL on old code, PASS on fixed code
        assertNotNull(result, "Result should not be null");
        assertEquals(expectedValue, result.getValue());
    }

    // ============================================
    // EDGE CASES (From RCA)
    // ============================================
    
    @Test
    @DisplayName("should handle edge case 1 from RCA")
    void shouldHandleEdgeCase1() {
        // Related scenario identified in root cause analysis
    }
    
    @Test
    @DisplayName("should handle edge case 2 from RCA")
    void shouldHandleEdgeCase2() {
        // Another related scenario
    }

    // ============================================
    // BOUNDARY CONDITIONS
    // ============================================
    
    @Test
    @DisplayName("should handle boundary condition")
    void shouldHandleBoundaryCondition() {
        // Test limits and boundaries
    }
    
    // ============================================
    // CACHE BEHAVIOR (LX-IWMS Specific)
    // ============================================
    
    @Test
    @DisplayName("should flush cache after update")
    void shouldFlushCacheAfterUpdate() {
        // Given
        int firmId = 123;
        
        // When
        serviceUnderTest.updateEntity(firmId, testData);
        
        // Then
        verify(mockFirmCache).flushCache(firmId, "EntityType");
    }
}
```

## Checklist Before Committing

- [ ] Test file is in correct location (`lx-common/src/test/java/` or `RolloutManager/junit/`)
- [ ] Test class has `@DisplayName` with ticket ID
- [ ] Package name matches module structure
- [ ] Regression test uses exact reproduction steps
- [ ] Edge cases from RCA are covered
- [ ] All tests pass with fix applied
- [ ] Mocks are used for external dependencies (FirmCache, EntityManager, etc.)
- [ ] No flaky/timing-dependent assertions
- [ ] Cache flush behavior is tested if applicable

## Running Tests

```powershell
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=EMS1234UserAuthTest

# Run tests in specific module
mvn test -pl RolloutManager -Dtest=EMS1234UserAuthTest
```

````
