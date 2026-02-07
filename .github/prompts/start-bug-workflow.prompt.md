---
name: start-bug-workflow
description: Start the complete bug-fixing workflow for a Jira ticket
agent: Bug Coordinator
argument-hint: Enter the Jira ticket key (e.g., EMS-1234)
---

# Start Bug Fixing Workflow

Start the complete bug-fixing workflow for ticket: $1

## Workflow Steps

This will guide you through:

1. **Fetch** - Get bug details from Jira
2. **Research** - Investigate the codebase
3. **Verify Research** - Check research accuracy
4. **Root Cause Analysis** - Identify why the bug occurs
5. **Verify RCA** - Validate the analysis
6. **Plan** - Create implementation plan
7. **Implement** - Fix the bug with tests

## Initial Action

Fetch the bug ticket from Jira and create the bug context document at:
`.context/bugs/$1/bug-context.md`

Then present options to proceed with the research phase.
