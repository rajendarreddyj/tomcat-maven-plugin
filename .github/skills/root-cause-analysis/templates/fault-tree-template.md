# Fault Tree Analysis: {TICKET-ID}

**Date**: {YYYY-MM-DD}
**Bug**: {Bug Title}

---

## Fault Tree Diagram

A fault tree is a top-down visual representation showing how different events combine to cause a fault. Use this to understand complex causal relationships.

```
                    [BUG SYMPTOM]
                    User sees 500 error
                           │
                           │
                    [IMMEDIATE CAUSE]
                    TypeError: Cannot read property 'amount'
                           │
                    ┌──────┴──────┐
                    │             │
            [FAULT POINT]    [CONTEXT]
            transaction is   Occurs at
            undefined        line 123
                    │
                    │
            [PROXIMATE CAUSE]
            createTransaction() returns undefined
                    │
            ┌───────┴────────┐
            │                │
        [TRIGGER]      [CONDITION]
        Amount > 1000  Fraud check enabled
                    │
                    │
            [ROOT CAUSE]
            Fraud check added in hotfix
            returns undefined instead of
            throwing error or using Result type
```

---

## Fault Tree Components

### Top Event (Bug Symptom)
**What**: [The observable problem]
**Evidence**: [User reports, logs, metrics]

### Intermediate Events
List each layer of causation from symptom down to root:

1. **Immediate Cause**: [Direct technical reason for symptom]
   - Evidence: `file:line`
   - Type: [Fault/Error/Exception]

2. **Proximate Cause**: [Why the immediate cause occurred]
   - Evidence: `file:line`
   - Type: [Logic Error/State Issue/etc.]

3. **Contributing Factors**: [Conditions that enabled the fault]
   - Factor A: [Description]
   - Factor B: [Description]

### Root Cause Event
**What**: [The fundamental cause that cannot be further decomposed]
**Category**: [Logic Error / State Management / etc.]
**Why This is Root**: [Explanation]

---

## Alternative Fault Tree: ASCII Format

For complex multi-causal bugs:

```
                                [BUG SYMPTOM]
                                    │
                    ┌───────────────┼───────────────┐
                    │               │               │
              [CAUSE 1]        [CAUSE 2]        [CAUSE 3]
                    │               │               │
            ┌───────┴──────┐        │        ┌──────┴──────┐
            │              │        │        │             │
      [SUB-CAUSE 1A] [SUB-CAUSE 1B] │  [SUB-CAUSE 3A] [SUB-CAUSE 3B]
                                     │
                              [ROOT CAUSE 2]
```

---

## AND/OR Gates

Use logic gates to show how multiple conditions combine:

### AND Gate (All conditions must be true)
```
                [FAULT]
                   │
            ┌──────┴──────┐
            │      AND     │
        ┌───┴───┐     ┌───┴───┐
    [COND A]        [COND B]
  (must be true)  (must be true)
```

### OR Gate (Any condition can trigger)
```
                [FAULT]
                   │
            ┌──────┴──────┐
            │      OR      │
        ┌───┴───┐     ┌───┴───┐
    [COND A]        [COND B]
  (either works)  (either works)
```

---

## Example: Complex Multi-Root-Cause Bug

```
                        [USER SEES INTERMITTENT CRASHES]
                                    │
                        ┌───────────┼───────────┐
                        │           │           │
                  [MEMORY LEAK] [RACE CONDITION] [NULL POINTER]
                        │           │           │
                        │     ┌─────┼─────┐     │
                        │     │    AND    │     │
                        │  [ASYNC] [SHARED]    │
                        │   CALL    STATE      │
                        │           │           │
                    [RC 1: No    [RC 2:       [RC 3: Missing
                     cleanup]    No locks]     validation]
```

---

## Using Fault Trees

### When to Create a Fault Tree

Create a fault tree when:
- Multiple contributing causes interact
- Complex causal chain needs visualization
- Communicating RCA to stakeholders
- Documenting incident for post-mortem

### How to Build

1. **Start at top** with the bug symptom
2. **Work downward** asking "What caused this?"
3. **Branch** when multiple causes contribute
4. **Use gates** (AND/OR) to show relationships
5. **Stop at root cause** (cannot decompose further)

### Integration with 5 Whys

Fault trees and 5 Whys are complementary:
- **5 Whys**: Linear descent for single-root-cause bugs
- **Fault Tree**: Visual map for multi-root-cause bugs

Use both when appropriate!

---

## Fault Tree Validation Checklist

- [ ] All paths lead from symptom to root cause
- [ ] Each node has evidence (file:line reference)
- [ ] AND/OR gates used correctly
- [ ] No circular dependencies
- [ ] Root causes are fundamental (cannot decompose further)
- [ ] All branches explored (no orphan causes)

---

## References

- [Fault Tree Analysis (FTA)](https://en.wikipedia.org/wiki/Fault_tree_analysis)
- [System Safety Analysis](https://www.nrc.gov/reading-rm/doc-collections/fact-sheets/fault-tree-analysis.html)
- Root Cause Analysis Skill: [SKILL.md](../SKILL.md)
