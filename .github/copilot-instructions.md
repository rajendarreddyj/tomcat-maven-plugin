# GitHub Copilot Instructions - tomcat-maven-plugin

## Project Overview

A Maven plugin for deploying and managing web applications on Apache Tomcat 10.1.x and 11.x.

- **Group ID:** `io.github.rajendarreddyj`
- **Artifact ID:** `tomcat-maven-plugin`
- **Target Tomcat Versions:** 10.1.x, 11.x

### Key Features

- Hot code deployment via Maven
- Environment variable configuration
- JVM options support
- JDK home configuration
- Catalina home support for existing Tomcat installations
- Auto-download Tomcat if catalinaHome not present
- Context path configuration
- Port configuration
- Auto-publish with inactivity detection
- Classpath additions support

### Reference Resources

- [Apache Tomcat Maven Plugin (Legacy)](https://github.com/apache/tomcat-maven-plugin)
- [Tomcat 10.1 Migration Guide](https://tomcat.apache.org/migration-10.1.html)
- [Tomcat 11.0 Migration Guide](https://tomcat.apache.org/migration-11.0.html)

## Code Modification and Contribution Guidelines

These instructions guide AI-assisted code contributions to ensure precision, maintainability, and alignment with project architecture.

1. **Minimize Scope of Change**
   - Identify the smallest unit (function, class, or module) that fulfills the requirement.
   - Do not modify unrelated code.
   - Avoid refactoring unless required for correctness or explicitly requested.

2. **Preserve System Behavior**
   - Ensure the change does not affect existing features or alter outputs outside the intended scope.
   - Maintain original patterns, APIs, and architectural structure unless otherwise instructed.

3. **Graduated Change Strategy**
   - **Default:** Implement the minimal, focused change.
   - **If Needed:** Apply small, local refactorings (e.g., rename a variable, extract a function).
   - **Only if Explicitly Requested:** Perform broad restructuring across files or modules.

4. **Clarify Before Acting on Ambiguity**
   - If the task scope is unclear or may impact multiple components, stop and request clarification.
   - Never assume broader intent beyond the described requirement.

5. **Log, Don't Implement, Unscoped Enhancements**
   - Identify and note related improvements without changing them.
   - Example: `// Note: This method could benefit from caching`

## Architecture Overview

### Libraries and Frameworks

- **Java:** JDK 21 (compile and runtime)
- **Maven Plugin API:** For Mojo development
- **Apache Tomcat:** 10.1.x and 11.x support
- **JUnit 5:** For unit testing
- **Mockito:** For mocking dependencies in tests

### Project Structure

```
tomcat-maven-plugin/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── io/github/rajendarreddyj/tomcat/
│   │   │       ├── AbstractTomcatMojo.java      # Base Mojo class
│   │   │       ├── RunMojo.java                 # tomcat:run goal
│   │   │       ├── StartMojo.java               # tomcat:start goal
│   │   │       ├── StopMojo.java                # tomcat:stop goal
│   │   │       ├── DeployMojo.java              # tomcat:deploy goal
│   │   │       ├── config/
│   │   │       │   ├── ServerConfiguration.java
│   │   │       │   ├── DeployableConfiguration.java
│   │   │       │   └── TomcatVersion.java
│   │   │       ├── download/
│   │   │       │   ├── TomcatDownloader.java
│   │   │       │   ├── TomcatVersionResolver.java
│   │   │       │   └── ChecksumValidator.java
│   │   │       ├── lifecycle/
│   │   │       │   ├── TomcatLauncher.java
│   │   │       │   ├── TomcatProcessMonitor.java
│   │   │       │   └── ShutdownHandler.java
│   │   │       └── deploy/
│   │   │           ├── ExplodedWarDeployer.java
│   │   │           ├── HotDeployWatcher.java
│   │   │           └── ContextReloader.java
│   │   └── resources/
│   │       └── META-INF/maven/plugin.xml
│   └── test/
│       └── java/
│           └── io/github/rajendarreddyj/tomcat/
├── pom.xml
└── requirements.md
```

### Plugin Configuration Parameters

```xml
<configuration>
    <!-- Tomcat Version & Location -->
    <tomcatVersion>10.1.52</tomcatVersion>
    <catalinaHome>${server.home.dir}</catalinaHome>
    <catalinaBase>${server.base.dir}</catalinaBase>

    <!-- Server Configuration -->
    <httpPort>8080</httpPort>
    <httpHost>localhost</httpHost>

    <!-- JVM Configuration -->
    <javaHome>${vm.install.path}</javaHome>
    <vmOptions>
        <vmOption>-Xmx1024m</vmOption>
    </vmOptions>
    <environmentVariables>
        <JAVA_OPTS>-Dfile.encoding=UTF-8</JAVA_OPTS>
    </environmentVariables>

    <!-- Deployment Configuration -->
    <contextPath>/myapp</contextPath>
    <deployDir>${server.deploy.dir}</deployDir>
    <warSourceDirectory>${project.build.directory}</warSourceDirectory>
    <deploymentOutputName>ROOT</deploymentOutputName>

    <!-- Auto-publish -->
    <autopublishEnabled>true</autopublishEnabled>
    <autopublishInactivityLimit>30</autopublishInactivityLimit>

    <!-- Timeouts -->
    <startupTimeout>1800000</startupTimeout>
    <shutdownTimeout>1800000</shutdownTimeout>

    <!-- Classpath -->
    <classpathAdditions>
        <classpathAddition>/path/to/extra.jar</classpathAddition>
    </classpathAdditions>
</configuration>
```

### Maven Plugin Development Patterns

**Mojo Implementation:**
```java
@Mojo(name = "run", requiresDependencyResolution = ResolutionScope.RUNTIME)
public class RunMojo extends AbstractTomcatMojo {

    @Parameter(property = "tomcat.http.port", defaultValue = "8080")
    private int httpPort;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        // Implementation
    }
}
```

**Parameter Injection:**
```java
@Parameter(property = "tomcat.catalina.home", required = true)
private File catalinaHome;

@Parameter(defaultValue = "${project}", readonly = true, required = true)
private MavenProject project;
```

### Configuration Classes

**ServerConfiguration** - Represents Tomcat server settings:

```java
public class ServerConfiguration {
    private Path catalinaHome;
    private Path catalinaBase;
    private String httpHost;
    private int httpPort;
    private Path javaHome;
    private List<String> vmOptions;
    private Map<String, String> environmentVariables;
    private long startupTimeout;
    private long shutdownTimeout;
}
```

**DeployableConfiguration** - Represents deployment settings:

```java
public class DeployableConfiguration {
    private String moduleName;
    private Path sourcePath;
    private String contextPath;
    private Path deployDir;
    private boolean autopublishEnabled;
    private int autopublishInactivityLimit;
}
```

**TomcatVersion** - Represents Tomcat version with download URLs:

```java
public enum TomcatVersion {
    TOMCAT_10_1("10.1", 11, "https://dlcdn.apache.org/tomcat/tomcat-10/"),
    TOMCAT_11("11", 17, "https://dlcdn.apache.org/tomcat/tomcat-11/");

    private final String majorMinor;
    private final int minimumJava;
    private final String downloadBaseUrl;

    public String getDownloadUrl(String fullVersion) {
        return downloadBaseUrl + "v" + fullVersion + "/bin/apache-tomcat-" + fullVersion + ".zip";
    }
}
```

### Tomcat Version Compatibility

**Tomcat 10.1.x:**
- Jakarta EE 10 (jakarta.* namespace)
- Requires Java 11+
- Servlet 6.0, JSP 3.1, EL 5.0, WebSocket 2.1

**Tomcat 11.x:**
- Jakarta EE 11
- Requires Java 17+
- Servlet 6.1, JSP 4.0, EL 6.0, WebSocket 2.2

### Tomcat Auto-Download

When `catalinaHome` is not specified or doesn't exist, the plugin downloads Tomcat:

**Download URL Pattern:**
```
https://dlcdn.apache.org/tomcat/tomcat-{MAJOR}/v{VERSION}/bin/apache-tomcat-{VERSION}.zip
```

**Fallback (Archive):**
```
https://archive.apache.org/dist/tomcat/tomcat-{MAJOR}/v{VERSION}/bin/apache-tomcat-{VERSION}.zip
```

**Checksum Verification:**
```
SHA-512: {DOWNLOAD_URL}.sha512
```

**Version Detection from Existing Installation:**
```java
// Read from lib/catalina.jar!/org/apache/catalina/util/ServerInfo.properties
// Property: server.number=10.1.52.0
```

## Code Style Guidelines

### Auto Generated Code

When generating code using GitHub Copilot, follow these guidelines:

1. Review the generated code for correctness, readability, and adherence to project standards.

### General Code Style

- **Use meaningful variable and function names.**
- **Indentation:** Use consistent indentation (4 spaces).
- **Line Length:** Keep lines under 120 characters.
- **Whitespace:** Use whitespace effectively to improve readability.
- **Avoid magic numbers:** Use constants instead of hardcoded values.

### Java

- **Utilize Modern Java Features:** Leverage streams, lambdas, functional interfaces, and other modern Java features.
- **Error Handling:** Use `MojoExecutionException` for recoverable errors, `MojoFailureException` for build failures. Consider using `Optional` to handle potential null values gracefully.
- **Collections Framework:** Choose appropriate data structures (e.g., `List`, `Set`, `Map`) based on the use case.
- **Testing:** Write unit and integration tests for Java code to ensure correctness and maintainability.
- **Avoid Deprecated APIs:** Suggest using modern replacements for deprecated APIs.
- **Logging:** Use Maven's built-in `Log` via `getLog()` method.

### Maven Plugin Conventions

- **Goal Naming:** Use lowercase, hyphen-separated names (`run`, `deploy`, `hot-deploy`)
- **Parameter Properties:** Prefix with `tomcat.` (e.g., `tomcat.http.port`)
- **Default Values:** Provide sensible defaults aligned with Tomcat conventions
- **Documentation:** Document all Mojo parameters with `@Parameter` Javadoc


## Common Gotchas

1. **Classpath Isolation:** Maven plugin classpath is separate from Tomcat classpath
2. **Process Management:** Properly handle Tomcat process lifecycle (start/stop/kill)
3. **File Locking:** Windows file locking issues during hot deployment
4. **Port Conflicts:** Validate port availability before starting Tomcat
5. **Timeout Handling:** Implement proper timeout handling for startup/shutdown operations

## AI-Generated Code Safety

- Verify all AI-suggested dependencies exist in Maven Central
- Confirm that generated code uses current Maven Plugin API (not deprecated methods)
- Ensure Tomcat API usage matches target versions (10.1.x/11.x)
- Validate file path handling works cross-platform (Windows/Linux/macOS)
- Cross-check Tomcat configuration options against official documentation
- Do not accept AI-generated justifications that contradict established security policies.

## Developer Tips

- **Reference Implementation:** Study the legacy `apache/tomcat-maven-plugin` for patterns
- **Testing:** Use embedded Tomcat for unit tests where possible. Use `maven-plugin-testing-harness` for integration tests.
- **Documentation:** Document all Mojo parameters with `@Parameter` Javadoc
- **Backwards Compatibility:** Maintain configuration compatibility where feasible
- **Error Messages:** Provide actionable error messages with configuration hints
- **Human Review is Essential:** Always review and refine the generated code.
- **Context is Key:** Provide Copilot with sufficient context (e.g., project structure, existing code, comments) to generate relevant and high-quality code.
- **Continuous Improvement:** Regularly review and adjust these instructions based on your team's specific needs and feedback.
- If you're working with input, assume it's hostile — validate and escape it.
- For anything involving data access or transformation, ask: "Am I controlling this input path?"
- If you're about to use a string to build a query, URL, or command — pause. There's probably a safer API.
- Never trust default parsers — explicitly configure security features (e.g. disable DTDs in XML).
- If something seems "too easy" with secrets or file I/O — it's probably unsafe.
- Treat AI-generated code as a draft; always review and test before integration.
- Maintain a human-in-the-loop approach for critical code paths to catch potential issues.
- Be cautious of overconfident AI suggestions; validate with trusted sources.
- Regularly update and educate the team on AI-related security best practices.

## Domain-Specific Research and Plans Location
- All research and plan files for various domains are organized within the `.context/domains/` directory and follow a consistent structure:
- All research and plan files are in markdown format (`.md`).
- All research and plan files should start with yyyy-mm-dd date followed by a brief descriptive title, separated by hyphens. For example: `2024-06-15-domain-research-topic.md`
- Use templates in .context/templates/research-template.md and .context/templates/plan-template.md for creating new research and plan files respectively.
- All research and plan files must include metadata at the top of the file in YAML format, containing at least the following fields:
  - `title`: A brief title of the research or plan.
  - `date`: The date of creation in `yyyy-mm-dd` format.
  - `author`: The name of the person who created the file.
- **Research Files:**
  - Current and archived research markdown files for each domain are located in:
    - `.context/domains/{domain-name}/research/`
  - **New research markdown files should be placed in:**
    - `.context/domains/{domain-name}/research/current/`
- **Plans Files:**
  - Current and archived plan markdown files for each domain are located in:
    - `.context/domains/{domain-name}/plans/`
  - **New plan markdown files should be created in:**
    - `.context/domains/{domain-name}/plans/active/`

> Replace `{domain-name}` with the actual domain name as appropriate.
