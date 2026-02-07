# GitHub Repository Rulesets

This directory contains repository ruleset configurations for the tomcat-maven-plugin project.

## Available Rulesets

### 1. Branch Protection (`branch-protection.json`)

Protects `main` and `develop` branches with the following rules:

- **Pull Request Requirements:**
  - Requires at least 1 approving review
  - Dismisses stale reviews when new commits are pushed
  - Requires approval of the most recent push
  - Requires all review threads to be resolved

- **Required Status Checks:**
  - Build and Test (ubuntu-latest, Java 21)
  - Build and Test (windows-latest, Java 21)
  - Build and Test (macos-latest, Java 21)
  - Code Quality check

- **Additional Protections:**
  - Prevents branch deletion
  - Prevents force pushes (non-fast-forward)
  - Requires linear history

### 2. Release Tag Protection (`tag-protection.json`)

Protects version tags (`v*`) with the following rules:

- Only administrators can create release tags
- Prevents tag deletion
- Prevents tag updates (moving tags)

## Applying Rulesets

### Option 1: GitHub Web UI

1. Go to repository **Settings** → **Rules** → **Rulesets**
2. Click **New ruleset** → **Import a ruleset**
3. Upload the JSON file or paste contents
4. Review and save

### Option 2: GitHub CLI

```bash
# Install GitHub CLI if not already installed
# https://cli.github.com/

# Import branch protection ruleset
gh api repos/{owner}/{repo}/rulesets \
  --method POST \
  --input .github/rulesets/branch-protection.json

# Import tag protection ruleset
gh api repos/{owner}/{repo}/rulesets \
  --method POST \
  --input .github/rulesets/tag-protection.json
```

### Option 3: GitHub API

```bash
curl -X POST \
  -H "Authorization: Bearer $GITHUB_TOKEN" \
  -H "Accept: application/vnd.github+json" \
  https://api.github.com/repos/{owner}/{repo}/rulesets \
  -d @.github/rulesets/branch-protection.json
```

## Bypass Actors

The rulesets are configured to allow repository administrators (RepositoryRole ID: 5) to bypass rules when necessary. This includes:

- Emergency hotfixes
- Initial repository setup
- Maintenance operations

## Customization

### Modifying Required Status Checks

Update the `required_status_checks` array in `branch-protection.json` to match your CI job names:

```json
{
  "context": "Your Job Name",
  "integration_id": null
}
```

### Adjusting Review Requirements

Modify the `pull_request` rule parameters:

```json
{
  "type": "pull_request",
  "parameters": {
    "required_approving_review_count": 2,  // Increase for stricter review
    "dismiss_stale_reviews_on_push": true,
    "require_code_owner_review": true,     // Enable CODEOWNERS requirement
    "require_last_push_approval": true,
    "required_review_thread_resolution": true
  }
}
```

## Related Files

- [CI Workflow](../workflows/ci.yml) - Defines the status checks referenced in rulesets
- [Publish Workflow](../workflows/publish.yml) - Release publishing workflow
- [CODEOWNERS](../CODEOWNERS) - Code ownership definitions (if enabled)

## References

- [GitHub Rulesets Documentation](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-rulesets/about-rulesets)
- [Rulesets API](https://docs.github.com/en/rest/repos/rules)
- [Repository Rules Schema](https://json.schemastore.org/github-ruleset.json)
