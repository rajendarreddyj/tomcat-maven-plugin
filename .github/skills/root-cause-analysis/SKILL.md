````markdown
---
name: root-cause-analysis
description: Methodology for performing root cause analysis on software bugs using 5 Whys technique. Use when trying to identify why a bug occurs, not just where.
license: MIT
compatibility: VS Code Insiders with GitHub Copilot
metadata:
  author: accruent
  version: "1.0"
  based-on: HumanLayer workflow patterns
---

# Root Cause Analysis

This skill provides RCA methodology and templates for systematic bug investigation using the 5 Whys technique.

## When to Use

Use this skill when:
- After codebase research has been verified
- When you need to understand **WHY** a bug occurs (not just WHERE)
- When proposing fix strategies with risk assessment
- When analyzing system failures or unexpected behavior

## Core Principles

### Focus on Causality, Not Location

**Wrong Approach**: "The bug is in `BOServiceMemberRequest.java:123` where member is null"
**Right Approach**: "The bug occurs because the service was designed for authenticated contexts but reused for public endpoints without defensive null checks"

### The Documentarian vs. The Analyst

- **Bug Researcher** (documentarian): Documents WHAT EXISTS without evaluation
- **RCA Analyst** (causal analyst): Analyzes WHY things behave the way they do
- **Bug Implementer** (engineer): Implements HOW to fix it

### Root Cause vs. Symptom

- **Symptom**: Observable manifestation of the bug
- **Fault**: Location where behavior deviates from expected
- **Root Cause**: Fundamental reason the fault exists

## The 5 Whys Methodology

### Overview

The 5 Whys is a technique to explore cause-and-effect relationships underlying a problem. By asking "Why?" repeatedly (typically 5 times), you drill down from symptoms to root causes.

### How to Apply 5 Whys

1. **Start with the Fault**: Begin with the specific location where behavior deviates
2. **Ask Why**: Why does this fault occur?
3. **Answer with Evidence**: Use verified research to answer, cite file:line
4. **Repeat**: Ask why that answer is true
5. **Stop at Root Cause**: When you reach a design decision, missing requirement, or system constraint

### Example: Null Pointer Bug in LX-IWMS

| # | Why? | Because... | Evidence |
|---|------|------------|----------|
| 1 | Why does it crash? | The `entity` object is null when accessing `.getName()` | `BOServiceEntityRequest.java:52` |
| 2 | Why is entity null? | The `FirmCache.getEntity()` returns null when entity was deleted | `FirmCache.java:78` |
| 3 | Why does cache return null? | The entity was deleted from another session but cache wasn't flushed | `BOFlushCache.java` not called |
| 4 | Why isn't cache flushed? | The delete operation doesn't call `BOFlushCache.flushCache()` | `BOServiceEntityRequest.java:120` |
| 5 | Why wasn't flush added? | **ROOT CAUSE**: The delete operation was added without implementing the distributed cache invalidation pattern. Developer assumed single-session usage, but system uses Hazelcast for distributed caching | Design gap + Hazelcast requirement |

**Root Cause**: The entity delete operation was designed for single-session usage but the system uses Hazelcast for distributed caching. The delete at `BOServiceEntityRequest.java:120` doesn't call `BOFlushCache.flushCache()` to invalidate across sessions.

### When to Stop

Stop asking "Why?" when you reach:
- **Design Decision**: "We chose this architecture because..."
- **Missing Requirement**: "This scenario wasn't considered in the spec"
- **System Constraint**: "This is limited by the framework/library"
- **External Factor**: "This depends on third-party behavior"

**Don't stop at**:
- Technical symptoms: "The variable is null"
- Code locations: "It happens in this function"
- Observations: "The API call fails"

## Root Cause Categories

Understanding common categories helps classification:

| Category | Description | LX-IWMS Examples |
|----------|-------------|------------------|
| **Logic Error** | Incorrect conditions, operators, off-by-one | Wrong BOService validation, boundary errors |
| **State Management** | Race conditions, stale state, missing updates | FirmCache/ThreadCache stale data, Hazelcast sync |
| **Resource Management** | Leaks, exhaustion, improper cleanup | Connection pool exhaustion, file handle leaks |
| **Data Issues** | Invalid input, type mismatch, encoding | JSP form validation gaps, SQL type mismatch |
| **Integration** | API changes, version mismatch, timing | CXF web service failures, Azure file storage |
| **Configuration** | Wrong settings, missing values, environment | `iwms.properties` issues, `web.xml` mapping |
| **Concurrency** | Deadlock, race condition, thread safety | `RMDaemonThread` conflicts, cache access |
| **Error Handling** | Swallowed exceptions, wrong error type | Empty catch blocks, generic exception handling |

## Fix Strategy Generation

After identifying root cause, generate multiple fix strategies:

### Strategy Components

Each strategy should document:

1. **Approach**: What would change
2. **Files Affected**: Specific file:line locations
3. **Implementation Complexity**: Low/Medium/High with reasoning
4. **Regression Risk**: Low/Medium/High with specific concerns
5. **Edge Cases**: What scenarios to consider
6. **Testing Strategy**: How to validate the fix
7. **Pros**: Benefits of this approach
8. **Cons**: Drawbacks or risks

### Generate Multiple Strategies

Always propose:
1. **Primary Strategy**: Recommended approach (usually simplest with lowest risk)
2. **Alternative Strategy 1**: If primary has risks or constraints
3. **Alternative Strategy 2**: Different approach or more comprehensive fix

### Risk Assessment Framework

**Complexity Levels:**
- **Low**: Single file, small change, well-understood area
- **Medium**: Multiple files, moderate change, some uncertainty
- **High**: Architecture change, many files, significant uncertainty

**Regression Risk Levels:**
- **Low**: Isolated change, limited usage, good test coverage
- **Medium**: Shared code, moderate usage, partial test coverage
- **High**: Core functionality, widely used, limited test coverage

## RCA Report Structure

Use the [rca-report-template.md](templates/rca-report-template.md) for consistent output:

1. **Executive Summary**: One-sentence root cause and fix
2. **Symptom Analysis**: Observable behavior and triggers
3. **Fault Localization**: Execution path to fault
4. **Root Cause Analysis**: 5 Whys table and statement
5. **Fix Strategies**: Multiple approaches with trade-offs
6. **Additional Considerations**: Risks, performance, migration

## Common Pitfalls

### Stopping Too Early

❌ **Symptom-Level Stop**: "The variable is undefined"
✅ **Root Cause**: "Input validation was omitted when this feature was rushed in v2.0, assuming upstream service would validate"

### Confusing Fault with Root Cause

❌ **Fault-Level**: "The null check is missing at line 45"
✅ **Root Cause**: "The function was copied from internal API code where null was impossible, now used in public API without adapting defensive checks"

### Proposing Vague Fixes

❌ **Vague**: "Add better error handling"
✅ **Specific**: "Wrap database call in try/catch at `BOServiceEntityRequest.java:78-82`, catch `SQLException`, return error response with user-friendly message"

## Quality Guidelines

### Root Cause Must Be Actionable

The root cause should point to something that can be addressed through code, configuration, or process changes.

❌ BAD: "Users make mistakes"
✅ GOOD: "Input validation doesn't prevent invalid email formats, allowing malformed data into the system"

### Evidence-Based Analysis

Every step in the 5 Whys must be backed by evidence from verified research.

❌ BAD: "Probably the cache isn't cleared"
✅ GOOD: "Cache invalidation at `FirmCache.java:123` only clears entries matching exact key, doesn't handle wildcard patterns per documentation at line 45-50"

### Consider Multiple Contributing Causes

Some bugs have multiple root causes that interact:

```markdown
## Root Causes (Multiple)

### Primary Root Cause
[Most significant cause]

### Contributing Root Cause 1
[Secondary cause that enables primary]

### Contributing Root Cause 2
[Another factor]

**Interaction**: [How these causes combine to create the bug]
```

## Templates

This skill includes templates for consistent documentation:

- [rca-report-template.md](templates/rca-report-template.md) - Main RCA report structure
- [fault-tree-template.md](templates/fault-tree-template.md) - Visual fault analysis

## Integration with Bug-Fixing Workflow

```
Jira Ticket
    ↓
Bug Context (bug-context.md)
    ↓
Codebase Research (codebase-research.md)
    ↓
Research Verification (verified-research.md)
    ↓
★ Root Cause Analysis (rca-report.md) ★  ← This skill
    ↓
RCA Verification (verified-rca.md)
    ↓
Implementation Plan
    ↓
Bug Fix Implementation
```

## References

- [5 Whys Technique](https://en.wikipedia.org/wiki/Five_whys) - Wikipedia overview
- [Root Cause Analysis in Software](https://www.atlassian.com/incident-management/postmortem/root-cause-analysis)
- HumanLayer workflow patterns
- Toyota Production System (origin of 5 Whys)

## Agent Integration

This skill is primarily used by:
- **RCA Analyst**: Performs the 5 Whys analysis
- **RCA Verifier**: Validates the analysis quality
- **Bug Planner**: Uses verified RCA to create implementation plans

````
