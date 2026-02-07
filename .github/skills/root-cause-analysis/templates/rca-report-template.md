````markdown
# Root Cause Analysis: {TICKET-ID}

**Date**: {YYYY-MM-DD}
**Analyst**: AI Agent (RCA Analyst)
**Bug**: {Bug Title from bug-context.md}
**Status**: RCA Complete - Pending Verification

---

## Executive Summary

**Root Cause**: [One-sentence statement of the fundamental cause]

**Recommended Fix**: [One-sentence description of the primary fix strategy]

**Risk Level**: [Low/Medium/High]

**Estimated Effort**: [Time estimate - e.g., 2-4 hours]

---

## Symptom Analysis

### Observable Behavior

[Describe what the user or system experiences. Be specific and factual.]

**Example:**
- Users clicking "Save" on the entity edit form receive a NullPointerException
- The error occurs 100% of the time when FirmCache returns null for the entity
- Log shows: `java.lang.NullPointerException at BOServiceEntityRequest.java:123`

### Trigger Conditions

[Describe when and how the bug occurs]

**Example:**
- **When**: After user edits entity and clicks "Save"
- **Preconditions**: Entity has been deleted from another session, cache not flushed
- **Environment**: Production only, not reproducible with fresh data

### Severity Assessment

**Level**: [Critical/High/Medium/Low]
**Impact**: [Describe the impact on users or system]
**Frequency**: [Always/Often/Sometimes/Rare]

**Example:**
- **Level**: High
- **Impact**: Blocks entity updates, data loss risk
- **Frequency**: Sometimes (when cache is stale)

---

## Fault Localization

### Entry Point

**File**: `RolloutManager/Java/com/lucernex/rolloutmanager/businessobject/BOServiceEntityRequest.java:XX`
**Trigger**: [How execution begins]

**Example:**
- **File**: `RolloutManager/Java/com/lucernex/rolloutmanager/businessobject/BOServiceEntityRequest.java:45`
- **Trigger**: User clicks "Save" on JSP form, BOEdit invokes `processUpdate()` via reflection

### Execution Path

[Trace the execution from entry point to fault location]

1. **Step 1**: `BOEdit.java:120` - Receives POST request, uses FormDescriptor to invoke methods
2. **Step 2**: `BOServiceEntityRequest.java:45` - Calls `validateEntity(data)`
3. **Step 3**: `FirmCache.java:78` - Attempts to retrieve entity from cache
4. **Fault Point**: `BOServiceEntityRequest.java:52` - Attempts to access `entity.getName()` but `entity` is null
5. **Symptom**: `NullPointerException` thrown, caught by error handler, returns error page

### Data Flow

| Step | Data In | Data Out | Transformation |
|------|---------|----------|----------------|
| JSP Form Submit | Form fields | `BOBaseEntityData` object | BOEdit reflection |
| Service receives | `BOBaseEntityData` | Entity lookup | Cache query |
| **Fault point** | Entity ID | `null` entity | Cache miss |
| Error handler | `NullPointerException` | Error page | Exception handling |

### Fault Details

**File**: `RolloutManager/Java/com/lucernex/rolloutmanager/businessobject/BOServiceEntityRequest.java:50-55`

**Fault Description**: [What happens at the fault location that deviates from expected behavior]

**Example:**
- **File**: `RolloutManager/Java/com/lucernex/rolloutmanager/businessobject/BOServiceEntityRequest.java:50-55`
- **Code**:
```java
Entity entity = FirmCache.getEntity(firmId, entityId);
String name = entity.getName(); // ← Fault: entity is null
return new BOBaseEntityData(name);
```
- **Expected**: `FirmCache.getEntity()` returns a valid Entity object
- **Actual**: `FirmCache.getEntity()` returns `null` when entity was deleted

---

## Root Cause Analysis (5 Whys)

| # | Why? | Because... | Evidence |
|---|------|------------|----------|
| 1 | Why does the code crash at line 52? | The `entity` variable is `null` when accessed | `BOServiceEntityRequest.java:52` |
| 2 | Why is `entity` null? | The `FirmCache.getEntity()` method returns `null` for deleted entities | `FirmCache.java:78-85` |
| 3 | Why does FirmCache return null? | The entity was deleted from another session but the cache wasn't flushed | `BOFlushCache.java` not called |
| 4 | Why wasn't the cache flushed? | The delete operation in another session doesn't broadcast cache invalidation | `BOServiceEntityRequest.deleteEntity():120` |
| 5 | Why doesn't it broadcast? | **ROOT CAUSE**: The delete operation was added without implementing `BOFlushCache.flushCache()` for distributed cache invalidation. The developer assumed single-session usage, but the system uses Hazelcast for distributed caching | Design gap + Hazelcast requirement |

### Root Cause Statement

**Category**: [Logic Error / State Management / Resource Management / Data Issue / Integration / Configuration / Concurrency / Error Handling]

**Root Cause**: [Clear, precise statement of the fundamental cause]

**Example:**
- **Category**: State Management + Error Handling
- **Root Cause**: The entity delete operation at `BOServiceEntityRequest.java:120` doesn't call `BOFlushCache.flushCache()` to invalidate the distributed Hazelcast cache. Combined with missing null-check before accessing the entity at line 52, this causes a NullPointerException when users in other sessions try to update a deleted entity.

**Why This is the Root Cause**: [Explanation of why we can't dig deeper]

**Example:**
This is the fundamental cause because it represents two gaps: (1) the delete operation didn't follow the established cache invalidation pattern used elsewhere in the codebase (e.g., `BOServiceProjectRequest.java:200`), and (2) defensive null-checking wasn't added when retrieving from cache. Both are addressable code changes.

---

## Fix Strategies

### Primary Strategy: Add Null Check and Cache Flush

**Approach**: Add null check before entity access AND add cache flush to delete operation.

**Files Affected**:
- `RolloutManager/Java/com/lucernex/rolloutmanager/businessobject/BOServiceEntityRequest.java:50-55` - Add null check
- `RolloutManager/Java/com/lucernex/rolloutmanager/businessobject/BOServiceEntityRequest.java:120-125` - Add cache flush

**Implementation**:
```java
// BOServiceEntityRequest.java:50-55 - Add null check
Entity entity = FirmCache.getEntity(firmId, entityId);
if (entity == null) {
    throw new EntityNotFoundException(
        "Entity " + entityId + " not found in firm " + firmId);
}
String name = entity.getName();
return new BOBaseEntityData(name);

// BOServiceEntityRequest.java:120-125 - Add cache flush
public void deleteEntity(int firmId, int entityId) {
    entityDAO.delete(entityId);
    BOFlushCache.flushCache(firmId, "Entity");
    ThreadCache.getInstance().clearCache();
}
```

**Risk Assessment**:
- **Complexity**: Low - straightforward null check and cache flush
- **Regression Risk**: Low - follows established patterns in codebase
- **Edge Cases**:
  - Ensure `EntityNotFoundException` is handled by error page
  - Verify cache flush doesn't cause performance issues
  - Check if other delete operations need similar fix

**Testing Strategy**:
- **Unit tests**: 
  - Test `processUpdate()` throws `EntityNotFoundException` when entity is null
  - Test `deleteEntity()` calls `BOFlushCache.flushCache()`
  - Test normal flow when entity exists
- **Integration tests**:
  - Delete entity in session A, update in session B → should show "not found" error
  - Delete entity, verify cache is cleared
- **Manual verification**:
  - Multi-user scenario with concurrent sessions
  - Verify error message is user-friendly

**Pros**:
- Follows existing error-handling patterns
- Makes failure explicit with clear error message
- Fixes root cause (cache invalidation) and symptom (null check)
- Easy to test and rollback

**Cons**:
- Adds overhead of cache flush on every delete (minimal)
- May need to update error handling in JSP

---

### Alternative Strategy 1: Cache-Only Fix

**Approach**: Only add cache flush to delete operation, relying on eventual consistency.

**Files Affected**:
- `RolloutManager/Java/com/lucernex/rolloutmanager/businessobject/BOServiceEntityRequest.java:120-125` - Add cache flush only

**Implementation**:
```java
// BOServiceEntityRequest.java:120-125
public void deleteEntity(int firmId, int entityId) {
    entityDAO.delete(entityId);
    BOFlushCache.flushCache(firmId, "Entity");
    ThreadCache.getInstance().clearCache();
}
```

**Risk Assessment**:
- **Complexity**: Low - single location change
- **Regression Risk**: Medium - doesn't handle race condition window
- **Edge Cases**: Race condition still possible during cache propagation

**Testing Strategy**: Same as primary, but edge cases may still fail during propagation delay

**Pros**:
- Minimal code change
- Fixes most scenarios

**Cons**:
- Race condition window still exists
- NullPointerException still possible in edge cases
- Doesn't provide user-friendly error message

---

### Alternative Strategy 2: Database Check Instead of Cache

**Approach**: Query database directly for update operations instead of relying on cache.

**Files Affected**:
- `RolloutManager/Java/com/lucernex/rolloutmanager/businessobject/BOServiceEntityRequest.java:50-55` - Use DAO instead of cache

**Implementation**:
```java
// BOServiceEntityRequest.java:50-55
Entity entity = entityDAO.findById(entityId);
if (entity == null) {
    throw new EntityNotFoundException(
        "Entity " + entityId + " not found");
}
String name = entity.getName();
return new BOBaseEntityData(name);
```

**Risk Assessment**:
- **Complexity**: Low - simple change
- **Regression Risk**: Medium - bypasses cache, may impact performance
- **Edge Cases**: None - database is source of truth

**Testing Strategy**: Same as primary

**Pros**:
- Eliminates cache consistency issue entirely
- Database is always source of truth
- Simpler logic

**Cons**:
- Bypasses cache, increases database load
- May impact performance for high-volume operations
- Doesn't fix the missing cache flush (other code still affected)

---

## Strategy Comparison

| Strategy | Complexity | Risk | Effort | User Experience | Recommended |
|----------|------------|------|--------|-----------------|-------------|
| Primary (Null Check + Cache Flush) | Low | Low | 2h | Clear error message | ✅ Yes |
| Alt 1 (Cache-Only) | Low | Medium | 1h | Generic 500 error | ❌ No |
| Alt 2 (Database Check) | Low | Medium | 1h | Clear error message | Only if cache issues persist |

## Recommendation

**Recommended Strategy**: Primary (Null Check + Cache Flush)

**Reasoning**: 
- Addresses both the symptom (null check) and root cause (cache invalidation)
- Follows established patterns in the codebase
- Provides clear user feedback
- Low risk and complexity

**Prerequisites**: 
- Verify `EntityNotFoundException` handling in error page JSP
- Check for other delete operations that may need similar cache flush

**Rollback Plan**: 
Revert the two file changes. The null check is safe to keep even if cache flush causes issues.

---

## Additional Considerations

### Regression Risks

**Areas That Might Be Affected**:
- Other operations that call `FirmCache.getEntity()`
- Delete operations in related entities
- Hazelcast cluster performance

**Mitigation**:
- Search codebase for all usages of `FirmCache.getEntity()` using `grep_search`
- Add null checks to critical paths
- Monitor cache hit rates after deployment

### Performance Impact

**Expected Impact**: Minimal - cache flush is lightweight operation

### LX-IWMS Specific Considerations

**Cache Pattern**: 
- Follow existing pattern in `BOServiceProjectRequest.java` for cache flush
- Use `ThreadCache.getInstance().clearCache()` for local cache

**BOEdit Reflection**:
- The null check will throw exception caught by BOEdit error handling
- Verify error message displays correctly in JSP form

### Monitoring Recommendations

**Metrics to Track After Deployment**:
- Frequency of `EntityNotFoundException` (should be rare)
- Reduction in `NullPointerException` (should drop to zero for this issue)
- Cache hit/miss rates in Hazelcast

**Alerts to Add**:
- Alert if `NullPointerException` in `BOServiceEntityRequest` occurs
- Alert if `EntityNotFoundException` spikes unexpectedly

---

## References

- Bug Context: `.context/bugs/{TICKET-ID}/bug-context.md`
- Verified Research: `.context/bugs/{TICKET-ID}/research/verified-research.md`
- Codebase Research: `.context/bugs/{TICKET-ID}/research/codebase-research.md`
- Cache Pattern Example: `RolloutManager/Java/.../BOServiceProjectRequest.java:200`
- Error Handling: `RolloutManager/Java/.../BOServiceRequestBase.java`

---

## Next Steps

1. ✅ RCA Complete
2. ⏳ **Verify RCA** → Have RCA Verifier validate this analysis
3. ⏳ Create Implementation Plan → Based on verified RCA
4. ⏳ Implement Fix → Execute the plan
5. ⏳ Validate Fix → Test and verify the fix works

````
