````markdown
# Codebase Research: {TICKET-ID}

**Date**: {YYYY-MM-DD}
**Researcher**: AI Agent (Bug Researcher)
**Bug**: {Title from bug-context.md}
**Status**: Research Complete - Pending Verification

---

## Research Summary

[2-3 sentence summary of what was discovered about the bug-related code. Focus on WHAT EXISTS, not what should change.]

---

## Detailed Findings

### Code Locations

| File | Lines | Component | Description |
|------|-------|-----------|-------------|
| `RolloutManager/Java/com/lucernex/rolloutmanager/businessobject/BOServiceEntity.java` | XX-YY | [name] | [What this code does] |
| `lx-common/Java/com/lucernex/common/util/Utility.java` | XX | [name] | [What this code does] |
| `lx-database/Java/com/lucernex/database/entity/Entity.java` | XX | [name] | [What this code does] |

### Code Flow Analysis

#### Entry Points
[How the bug-related code is triggered]

- `BOServiceEntityRequest.java:XX` - [Description of trigger]
- `EntityController.java:YY` - [Alternative entry point via JSP/Servlet]

#### Execution Flow

```
[User action / JSP form submit]
    ↓
BOEdit.java (reflection-based method invocation)
    ↓
BOServiceEntityRequest.java:XX → processEntity()
    ↓
FirmCache.java:YY → getEntity()
    ↓
EntityDAO.java:ZZ → database query
    ↓
[output/result]
```

#### Dependencies

| Dependency | Location | Purpose |
|------------|----------|---------|
| FirmCache | `RolloutManager/Java/.../FirmCache.java:XX` | [How it's used] |
| ThreadCache | `lx-common/Java/.../ThreadCache.java:YY` | [What it provides] |
| SQL Server | `lx-sql/DbSchema/TableName.sql` | [Database table] |

#### Error Handling

| Location | Error Type | Handling |
|----------|------------|----------|
| `BOServiceEntity.java:XX` | NullPointerException | [What happens when error occurs] |
| `EntityDAO.java:YY` | SQLException | [What happens when error occurs] |

### Related Patterns

#### Similar Code
[Other places in the codebase with similar patterns - for context, not comparison]

| File | Lines | Similarity |
|------|-------|------------|
| `RolloutManager/Java/.../BOServiceOtherEntity.java` | XX-YY | [How it's similar - factually] |

#### Related Tests

| Test File | Line | What It Tests |
|-----------|------|---------------|
| `RolloutManager/junit/com/lucernex/.../BOServiceEntityTest.java` | XX | [Description of test] |
| `lx-common/src/test/java/.../UtilityTest.java` | YY | [Description of test] |

#### Related JSP/UI Components

| JSP File | Line | Purpose |
|----------|------|---------|
| `RolloutManager/JSP/entity/edit.jsp` | XX | [Form that triggers this flow] |
| `RolloutManager/JSP/WEB-INF/tags/entityField.tag` | YY | [Custom tag used] |

#### Historical Context
[From git history if available - factual observations only]

- Commit `abc123` (YYYY-MM-DD): "[commit message]" - modified `BOServiceEntity.java`
- PR #XXX: "[title]" - relevant context

---

## Code Snippets

### {Component 1 Name}
**File**: `RolloutManager/Java/com/lucernex/rolloutmanager/businessobject/BOServiceEntity.java:XX-YY`
```java
// EXACT code from source - copied verbatim
[code snippet]
```

### {Component 2 Name}
**File**: `lx-common/Java/com/lucernex/common/util/Utility.java:XX-YY`
```java
// EXACT code from source - copied verbatim
[code snippet]
```

### {Database Schema (if relevant)}
**File**: `lx-sql/DbSchema/EntityTable.sql`
```sql
-- EXACT schema from source
[sql schema]
```

---

## LX-IWMS Specific Patterns Found

### Business Object Pattern
[If the bug involves BOService* classes, document the pattern usage]

- Base class: `BOServiceRequestBase`
- Data class: `BOBase*Data`
- Fetch methods: Generated via Perl scripts

### Cache Usage
[Document any cache-related code paths]

- FirmCache access at: `file:line`
- Cache flush at: `file:line`
- ThreadCache usage at: `file:line`

### BOEdit Reflection
[If JSP CRUD operations are involved - document the reflection chain]

- FormDescriptor at: `file:line`
- BOBeanField mapping at: `file:line`
- setOverrideValue call at: `file:line`

---

## Open Questions

[Areas that couldn't be fully researched - questions for the verifier or RCA phase]

1. [Question about behavior that couldn't be determined]
2. [Question about edge case not found in code]
3. [Question about configuration or environment]

---

## References

- Bug Context: `.context/bugs/{TICKET-ID}/bug-context.md`
- Hypotheses: `.context/bugs/{TICKET-ID}/research/hypothesis.md`
- Project Architecture: `.github/copilot-instructions.md`
- BOEdit Reflection Pattern: `.github/instructions/boedit-reflection.instructions.md`

````
