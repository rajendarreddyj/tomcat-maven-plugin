---
description: Fetch a bug ticket from Jira and create a bug context document for the bug-fixing workflow
argument-hint: Enter the Jira ticket key (e.g., EMS-1234)
---

# Fetch Bug from Jira

Fetch Jira ticket **$1** and create a structured bug context document.

## Instructions

You are fetching a bug ticket from Jira to prepare for the bug-fixing workflow. Follow these steps carefully:

### Step 1: Fetch the Ticket

Use `mcp_atlassian-mcp_search` to search for the ticket:

```
Query: "$1"
```

If the search returns no results or an error, report the issue and stop.

### Step 2: Create Directory Structure

Create the following directory structure:
- `.context/bugs/$1/`
- `.context/bugs/$1/research/`

### Step 3: Extract Information

From the Jira response, extract:
- **Title/Summary**: The bug title
- **Description**: Full description with formatting preserved
- **Status**: Current workflow status
- **Priority**: Bug priority
- **Reporter**: Who reported it
- **Created Date**: When it was created
- **Assignee**: Who it's assigned to (if any)
- **Comments**: All comments in chronological order
- **Attachments**: List of attachments with metadata
- **Labels/Components**: Any categorization
- **Sprint**: Current sprint if applicable

### Step 4: Parse Structured Information

Look for these patterns in the description:
- **Steps to Reproduce**: Often numbered lists or "Steps:" section
- **Expected Behavior**: Look for "Expected:" or "Should:" patterns
- **Actual Behavior**: Look for "Actual:" or "Instead:" or "Bug:" patterns
- **Environment**: Browser, OS, version information

If these aren't clearly separated, do your best to extract them from the description.

### Step 5: Create Bug Context Document

Create the file `.context/bugs/$1/bug-context.md` with the following format:

```markdown
# Bug: $1

**Title:** [summary]
**Status:** [status]
**Priority:** [priority]
**Created:** [created_date]
**Reporter:** [reporter]
**Assignee:** [assignee or "Unassigned"]

---

## Description

[Full description - preserve original formatting]

---

## Steps to Reproduce

[Extracted or from custom field]

1. 
2. 
3. 

---

## Expected Behavior

[What should happen]

---

## Actual Behavior

[What actually happens - the bug symptom]

---

## Environment

[Environment details if available]

---

## Attachments

| Filename | Type | Size | Notes |
|----------|------|------|-------|
| [list each attachment] |

---

## Comments

### [author] - [timestamp]
[comment body]

[repeat for each comment]

---

## Metadata

- **Jira Link:** [url to ticket]
- **Fetched:** [current date/time]
- **Labels:** [labels]
- **Components:** [components]
```

### Step 6: Report Completion

After creating the file, report:

```
✅ Bug context created successfully!

**Ticket:** $1
**Title:** [title]
**Status:** [status]
**Priority:** [priority]

**Output file:** .context/bugs/$1/bug-context.md

**Next steps:**
1. Review the generated bug context for accuracy
2. Add any missing information from attachments
3. Use @bug-researcher to investigate the codebase (when available)
   OR manually research the codebase based on the bug description
```

## Error Handling

If the ticket cannot be found:
```
❌ Could not fetch ticket: $1

Possible issues:
- Ticket key may be incorrect
- You may not have access to this project
- Atlassian MCP may not be connected

Please verify the ticket key and your MCP connection.
```

## Notes

- This prompt uses the Atlassian MCP tools - ensure they are configured
- The output follows the template from the jira-bug-fetcher skill
- Attachments are listed but not downloaded - review them manually if needed
