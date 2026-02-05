# Publishing to Maven Central

This guide explains how to publish the `maven-tomcat-plugin` to Maven Central via Sonatype OSSRH.

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

1. **Update version in `pom.xml`** (remove `-SNAPSHOT`):
   ```xml
   <version>1.0.0</version>
   ```

2. **Commit and push** the version change:
   ```bash
   git add pom.xml
   git commit -m "Release version 1.0.0"
   git push origin main
   ```

3. **Create a GitHub Release**:
   - Go to Releases → Draft a new release
   - Create a new tag: `v1.0.0`
   - Title: `Release 1.0.0`
   - Add release notes
   - Click "Publish release"

4. The `publish.yml` workflow will automatically:
   - Build the project
   - Sign artifacts with GPG
   - Deploy to Maven Central
   - Upload release artifacts

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

- [ ] All tests pass: `mvn clean verify`
- [ ] Coverage thresholds met (90% line, 85% branch)
- [ ] Version number updated (no `-SNAPSHOT`)
- [ ] `README.md` updated with new version
- [ ] CHANGELOG updated with release notes
- [ ] All breaking changes documented

## Version Numbering

Follow [Semantic Versioning](https://semver.org/):

- **MAJOR** (`X.0.0`): Breaking API changes
- **MINOR** (`0.X.0`): New features, backward compatible
- **PATCH** (`0.0.X`): Bug fixes, backward compatible

## Post-Release Steps

After successful publication:

1. **Verify on Maven Central**:
   - Search for the artifact at [search.maven.org](https://search.maven.org/search?q=g:io.github.rajendarreddyj%20AND%20a:maven-tomcat-plugin)
   - Note: It may take up to 4 hours to appear in search

2. **Update to next SNAPSHOT**:
   ```xml
   <version>1.0.1-SNAPSHOT</version>
   ```

3. **Push the snapshot version**:
   ```bash
   git add pom.xml
   git commit -m "Prepare for next development iteration"
   git push origin main
   ```

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
            <artifactId>maven-tomcat-plugin</artifactId>
            <version>1.0.0</version>
        </plugin>
    </plugins>
</build>
```

Or use the plugin directly:

```bash
mvn io.github.rajendarreddyj:maven-tomcat-plugin:1.0.0:run
```
