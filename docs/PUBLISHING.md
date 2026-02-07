# Publishing to Maven Central

This guide explains how to publish the `tomcat-maven-plugin` to Maven Central via Sonatype OSSRH.

## GitHub Flow

This project follows [GitHub Flow](https://docs.github.com/en/get-started/quickstart/github-flow), a lightweight branch-based workflow:

### Branching Model

- **`main`** - The single long-lived branch, always in a deployable state
- **Feature branches** - Short-lived branches for all changes (e.g., `feature/add-hot-deploy`, `fix/port-conflict`)

### Development Workflow

1. **Create a feature branch** from `main`:
   ```bash
   git checkout main
   git pull origin main
   git checkout -b feature/your-feature-name
   ```

2. **Make changes** and commit frequently:
   ```bash
   git add .
   git commit -m "Add feature description"
   ```

3. **Push and create a Pull Request**:
   ```bash
   git push -u origin feature/your-feature-name
   ```
   - Open a PR targeting `main`
   - CI workflow runs automatically on PRs

4. **Merge after review** - PR merges into `main` after approval and CI passes

5. **Release from `main`** - Create a GitHub Release to publish to Maven Central

### CI/CD Workflows

| Workflow | Trigger | Purpose |
|----------|---------|---------|
| `ci.yml` | Push to `main`, PRs to `main` | Build, test, code quality checks |
| `publish.yml` | GitHub Release publish | Deploy to Maven Central |

## Automated Publishing Process

The entire publishing process is automated via GitHub Actions. Once configured, publishing requires only creating a GitHub Release.

### What Gets Automated

When you publish a GitHub Release, the `publish.yml` workflow automatically:

1. **Generates release notes** from commits since the last tag
2. **Updates the GitHub Release** with the generated changelog
3. **Sets the version** from the release tag (removes `v` prefix)
4. **Builds and verifies** the project
5. **Signs artifacts** with GPG
6. **Deploys to Maven Central**
7. **Uploads artifacts** to the GitHub Release

### Automatic Release Notes

The workflow automatically generates release notes containing:
- All commits since the previous tag
- Link to full changelog comparison

**Example output:**
```markdown
## What's Changed

- Add hot deploy feature (a1b2c3d)
- Fix port conflict detection (e4f5g6h)
- Update dependencies (i7j8k9l)

**Full Changelog**: https://github.com/rajendarreddyj/tomcat-maven-plugin/compare/v0.9.0...v1.0.0
```

### Quick Release Steps

1. **Ensure `main` is ready** - all features merged, tests passing
2. **Create GitHub Release**:
   ```bash
   # Option A: Using Git CLI
   git tag v1.0.0
   git push origin v1.0.0
   # Then create release from tag in GitHub UI

   # Option B: Using GitHub CLI
   gh release create v1.0.0 --title "Release 1.0.0" --generate-notes
   ```
3. **Monitor the workflow** - Actions tab shows progress
4. **Verify on Maven Central** - available within ~10-15 minutes

### Version Handling

- **Tag format**: `v1.0.0` (with `v` prefix)
- **Maven version**: `1.0.0` (without `v` prefix, extracted automatically)
- **No manual `pom.xml` changes needed** - version is set from tag during build

## Prerequisites

### 1. Sonatype OSSRH Account

1. Create an account at [Sonatype JIRA](https://issues.sonatype.org/secure/Signup!default.jspa)
2. Create a New Project ticket requesting access to the `io.github.rajendarreddyj` namespace
3. Wait for approval (usually within 2 business days)

### 2. GPG Key Setup

Generate a GPG key for signing artifacts:

```bash
# Generate a new GPG key
gpg --full-generate-key
```

When prompted, select:
1. **Key type**: `1` (RSA and RSA - default and recommended)
2. **Key size**: `4096` (stronger security, required for Maven Central)
3. **Expiration**: `0` for no expiration, or specify duration (e.g., `2y` for 2 years)
4. **Real name**: Your name (e.g., `RajendarReddy Jagapathi`)
5. **Email**: Your email (e.g., `rajendarreddyj@gmail.com`)
6. **Comment**: Optional, can leave blank
7. **Passphrase**: Choose a strong passphrase (save this for `GPG_PASSPHRASE` secret)

```bash
# List keys to get the key ID
gpg --list-secret-keys --keyid-format=long

# Export the public key to a keyserver
gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>

# Export the private key (for CI/CD)
gpg --armor --export-secret-keys <KEY_ID> > private-key.asc
```

### 3. Generate Sonatype Token

1. Log in to [Sonatype Portal](https://central.sonatype.com/)
2. Navigate to Account → Generate User Token
3. Save the username and token securely

## GitHub Repository Setup

### Required Secrets

Configure these secrets in your GitHub repository (Settings → Secrets and variables → Actions):

| Secret Name | Description | Example |
|-------------|-------------|---------|
| `OSSRH_USERNAME` | Sonatype portal token username | `l3x7k9...` |
| `OSSRH_TOKEN` | Sonatype portal token password | `ghp_...` |
| `GPG_PRIVATE_KEY` | ASCII-armored GPG private key | Contents of `private-key.asc` |
| `GPG_PASSPHRASE` | GPG key passphrase | Your GPG passphrase |

### Create GitHub Environment

1. Go to Settings → Environments
2. Create a new environment named `maven-central`
3. (Optional) Add required reviewers for release protection
4. Add the secrets listed above to this environment

## Publishing Methods

### Method 1: GitHub Release (Recommended)

The simplest and recommended approach - just create a release:

1. **Create a GitHub Release**:
   - Go to Releases → Draft a new release
   - Create a new tag: `v1.0.0` (from `main` branch)
   - Title: `Release 1.0.0`
   - Description is optional (auto-generated from commits)
   - Click "Publish release"

2. **That's it!** The workflow automatically:
   - Extracts version `1.0.0` from tag `v1.0.0`
   - Generates release notes from commits
   - Builds, signs, and deploys to Maven Central
   - Uploads JAR artifacts to the release

**Using GitHub CLI:**
```bash
gh release create v1.0.0 --title "Release 1.0.0" --generate-notes
```

**Using Git + GitHub UI:**
```bash
git tag v1.0.0
git push origin v1.0.0
# Then go to GitHub → Releases → create release from tag
```

### Method 2: Manual Workflow Dispatch

1. Go to Actions → "Publish to Maven Central"
2. Click "Run workflow"
3. Optionally specify a version (leave empty to use pom.xml version)
4. Click "Run workflow"

### Method 3: Local Publishing

For testing or manual releases:

```bash
# Set environment variables
export MAVEN_USERNAME="your-ossrh-username"
export MAVEN_PASSWORD="your-ossrh-token"

# Build and deploy
mvn clean deploy -Prelease

# Or deploy without tests (if already verified)
mvn deploy -Prelease -DskipTests
```

## Release Checklist

Before publishing a release:

- [ ] All feature PRs merged to `main`
- [ ] All tests pass: `mvn clean verify`
- [ ] Coverage thresholds met (90% line, 85% branch)
- [ ] `README.md` updated with new version (if needed)
- [ ] All breaking changes documented in commit messages

After creating the release:
- [ ] Verify workflow completed successfully in Actions tab
- [ ] Verify artifact on Maven Central (~10-15 minutes)

## Version Numbering

Follow [Semantic Versioning](https://semver.org/):

- **MAJOR** (`X.0.0`): Breaking API changes
- **MINOR** (`0.X.0`): New features, backward compatible
- **PATCH** (`0.0.X`): Bug fixes, backward compatible

## Post-Release Steps

After successful publication:

1. **Verify on Maven Central**:
   - Search for the artifact at [search.maven.org](https://search.maven.org/search?q=g:io.github.rajendarreddyj%20AND%20a:tomcat-maven-plugin)
   - Note: It may take up to 4 hours to appear in search (usually ~10-15 minutes)

2. **Verify GitHub Release**:
   - Check that release notes were auto-generated
   - Check that JAR artifacts were uploaded

## Troubleshooting

### Common Issues

#### GPG Signing Fails
```
gpg: signing failed: No secret key
```
**Solution**: Ensure `GPG_PRIVATE_KEY` secret contains the full ASCII-armored key including headers.

#### Authentication Failed
```
401 Unauthorized
```
**Solution**: Regenerate Sonatype token and update `OSSRH_USERNAME` and `OSSRH_TOKEN` secrets.

#### Validation Errors
```
Invalid POM: Missing required fields
```
**Solution**: Ensure `pom.xml` has all required elements:
- `name`, `description`, `url`
- `licenses`
- `developers`
- `scm`

#### Duplicate Version
```
Repository does not allow updating assets
```
**Solution**: You cannot republish the same version. Increment the version number.

### Getting Help

- [Sonatype OSSRH Guide](https://central.sonatype.org/publish/publish-guide/)
- [Maven Central Requirements](https://central.sonatype.org/publish/requirements/)
- [GPG Signing Guide](https://central.sonatype.org/publish/requirements/gpg/)

## Maven Central Usage

After publishing, users can add the plugin to their project:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>io.github.rajendarreddyj</groupId>
            <artifactId>tomcat-maven-plugin</artifactId>
            <version>1.0.0</version>
        </plugin>
    </plugins>
</build>
```

Or use the plugin directly:

```bash
mvn io.github.rajendarreddyj:tomcat-maven-plugin:1.0.0:run
```
