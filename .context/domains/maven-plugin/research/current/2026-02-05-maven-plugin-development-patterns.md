---
title: Maven Plugin Development Best Practices and Patterns
date: 2026-02-05
author: AI Research Assistant
sources:
  - https://maven.apache.org/guides/plugin/guide-java-plugin-development.html
  - https://maven.apache.org/plugin-developers/index.html
  - https://maven.apache.org/ref/current/maven-plugin-api/
  - https://maven.apache.org/developers/mojo-api-specification.html
  - https://maven.apache.org/plugin-developers/plugin-testing.html
  - https://maven.apache.org/plugin-developers/common-bugs.html
  - https://maven.apache.org/maven-jsr330.html
  - https://maven.apache.org/plugin-tools/maven-plugin-tools-annotations/index.html
---

# Maven Plugin Development Best Practices and Patterns

## 1. Key Annotations and Their Usage

### 1.1 @Mojo Annotation

The `@Mojo` annotation is **required** and marks a class as a Maven goal. Minimum requirement: `@Mojo(name = "<goal-name>")`.

```java
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.plugins.annotations.InstantiationStrategy;

@Mojo(
    name = "<goal-name>",                                    // Required: goal name
    aggregator = <false|true>,                               // Run once for reactor
    defaultPhase = LifecyclePhase.<phase>,                   // Default lifecycle phase
    requiresDependencyResolution = ResolutionScope.<scope>,  // Dependency resolution scope
    requiresDependencyCollection = ResolutionScope.<scope>,  // Since Maven 3.0
    requiresOnline = <false|true>,                           // Requires network access
    requiresProject = <true|false>,                          // Requires Maven project
    threadSafe = <false|true>,                               // Since Maven 3.0
    instantiationStrategy = InstantiationStrategy.<strategy>,
    configurator = "<role hint>"
)
```

**@Mojo Attributes:**

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| `name` | String | **Required** | The goal name |
| `defaultPhase` | LifecyclePhase | none | Default phase to bind to |
| `requiresDependencyResolution` | ResolutionScope | none | `COMPILE`, `RUNTIME`, `TEST`, `COMPILE_PLUS_RUNTIME` |
| `requiresDependencyCollection` | ResolutionScope | none | Lighter than resolution (Maven 3.0+) |
| `requiresProject` | boolean | true | Whether goal requires a project |
| `requiresOnline` | boolean | false | Whether network access is required |
| `aggregator` | boolean | false | Run once for entire reactor, not per module |
| `threadSafe` | boolean | false | Whether mojo is thread-safe for parallel builds |
| `instantiationStrategy` | InstantiationStrategy | PER_LOOKUP | `PER_LOOKUP`, `SINGLETON`, `KEEP_ALIVE`, `POOLABLE` |

### 1.2 @Execute Annotation

Used to fork a lifecycle or goal execution before the annotated mojo runs.

```java
@Execute(
    goal = "<goal-name>",           // Goal to execute
    phase = LifecyclePhase.<phase>, // Phase to execute to
    lifecycle = "<lifecycle-id>"    // Custom lifecycle
)
```

### 1.3 @Parameter Annotation

Defines configurable mojo parameters. Can be applied to fields or setter methods.

```java
import org.apache.maven.plugins.annotations.Parameter;

@Parameter(
    name = "parameter",                    // Parameter name (defaults to field name)
    alias = "myAlias",                     // Alternative configuration name
    property = "a.property",               // System property name (-D option)
    defaultValue = "${project.basedir}",   // Default value (supports expressions)
    readonly = <false|true>,               // Cannot be configured by user
    required = <false|true>                // Must be provided
)
private String parameter;
```

**@Parameter Attributes:**

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| `name` | String | field name | Parameter name in configuration |
| `alias` | String | none | Alternative name for configuration |
| `property` | String | none | System property for `-D` configuration |
| `defaultValue` | String | none | Default value, supports `${expressions}` |
| `required` | boolean | false | Whether parameter must be configured |
| `readonly` | boolean | false | Cannot be configured in POM |

**Common Parameter Expressions:**

| Expression | Description |
|------------|-------------|
| `${project}` | MavenProject instance |
| `${project.basedir}` | Project base directory |
| `${project.build.directory}` | Target directory |
| `${project.version}` | Project version |
| `${session}` | MavenSession instance |
| `${settings}` | Settings instance |
| `${plugin}` | PluginDescriptor instance |
| `${mojoExecution}` | MojoExecution instance |
| `${reactorProjects}` | List of reactor projects |

### 1.4 Deprecated: @Component Annotation

Legacy annotation for injecting Plexus components. Use JSR-330 `@Inject` instead.

```java
// Legacy Plexus way (deprecated)
@Component(role = MyService.class, hint = "default")
private MyService service;

// Modern JSR-330 way (preferred)
@Inject
private MyService service;
```

## 2. Mojo Lifecycle and Execution Patterns

### 2.1 Basic Mojo Structure

```java
package sample.plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "sayhi")
public class GreetingMojo extends AbstractMojo {
    
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Hello, world.");
    }
}
```

### 2.2 Mojo Requirements

1. Must have an `execute()` method with no parameters and void return type
2. `execute()` must throw `MojoExecutionException` or `MojoFailureException` (or `RuntimeException`)
3. Must declare fields for each goal parameter
4. Must be accompanied by `META-INF/maven/plugin.xml` descriptor (auto-generated)

### 2.3 Lifecycle Phase Binding

```java
@Mojo(name = "compile-check", defaultPhase = LifecyclePhase.COMPILE)
public class CompileCheckMojo extends AbstractMojo {
    @Override
    public void execute() throws MojoExecutionException {
        // Automatically runs during compile phase
    }
}
```

**Available LifecyclePhase values:**

- `VALIDATE`, `INITIALIZE`, `GENERATE_SOURCES`, `PROCESS_SOURCES`
- `GENERATE_RESOURCES`, `PROCESS_RESOURCES`, `COMPILE`, `PROCESS_CLASSES`
- `GENERATE_TEST_SOURCES`, `PROCESS_TEST_SOURCES`, `GENERATE_TEST_RESOURCES`
- `PROCESS_TEST_RESOURCES`, `TEST_COMPILE`, `PROCESS_TEST_CLASSES`
- `TEST`, `PREPARE_PACKAGE`, `PACKAGE`, `PRE_INTEGRATION_TEST`
- `INTEGRATION_TEST`, `POST_INTEGRATION_TEST`, `VERIFY`, `INSTALL`, `DEPLOY`
- `PRE_CLEAN`, `CLEAN`, `POST_CLEAN`
- `PRE_SITE`, `SITE`, `POST_SITE`, `SITE_DEPLOY`

### 2.4 Dependency Resolution Scopes

```java
@Mojo(
    name = "analyze",
    requiresDependencyResolution = ResolutionScope.RUNTIME
)
```

**ResolutionScope values:**

| Scope | Includes |
|-------|----------|
| `COMPILE` | compile-scoped dependencies |
| `RUNTIME` | compile + runtime dependencies |
| `TEST` | compile + runtime + test dependencies |
| `COMPILE_PLUS_RUNTIME` | compile + runtime (no test) |
| `NONE` | No dependency resolution |

## 3. Dependency Injection Patterns

### 3.1 JSR-330 Injection (Recommended for Maven 3.1+)

```java
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Mojo(name = "hello", defaultPhase = LifecyclePhase.VALIDATE, requiresProject = false)
public class Jsr330Mojo extends AbstractMojo {

    private final Jsr330Component component;
    private final MavenSession session;
    private final MavenProject project;
    private final MojoExecution mojoExecution;

    @Inject
    public Jsr330Mojo(
            Jsr330Component component,
            MavenSession session,
            MavenProject project,
            MojoExecution mojoExecution) {
        this.component = component;
        this.session = session;
        this.project = project;
        this.mojoExecution = mojoExecution;
    }

    public void execute() throws MojoExecutionException {
        component.hello();
    }
}
```

### 3.2 JSR-330 Component Definition

```java
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Named
@Singleton
public class Jsr330Component {
    
    public void hello() {
        System.out.println("Hello from JSR-330 component!");
    }
}
```

### 3.3 POM Configuration for JSR-330

```xml
<dependencies>
    <dependency>
        <groupId>javax.inject</groupId>
        <artifactId>javax.inject</artifactId>
        <version>1</version>
        <scope>provided</scope>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.eclipse.sisu</groupId>
            <artifactId>sisu-maven-plugin</artifactId>
            <version>0.3.5</version>
            <executions>
                <execution>
                    <id>generate-index</id>
                    <goals>
                        <goal>main-index</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

### 3.4 Lifecycle Annotations (JSR-250)

```java
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Named
public class MyComponent {
    
    @PostConstruct
    public void init() {
        // Called after construction and injection
    }
    
    @PreDestroy
    public void cleanup() {
        // Called before destruction
    }
}
```

## 4. Parameter Configuration Patterns

### 4.1 Simple Parameters

```java
@Parameter(property = "greeting", defaultValue = "Hello World!")
private String greeting;

@Parameter(property = "outputDirectory", defaultValue = "${project.build.directory}")
private File outputDirectory;

@Parameter(property = "skip", defaultValue = "false")
private boolean skip;
```

### 4.2 Read-Only Parameters (Injected Context)

```java
@Parameter(defaultValue = "${project}", readonly = true, required = true)
private MavenProject project;

@Parameter(defaultValue = "${session}", readonly = true, required = true)
private MavenSession session;

@Parameter(defaultValue = "${mojoExecution}", readonly = true)
private MojoExecution mojoExecution;

@Parameter(defaultValue = "${settings}", readonly = true)
private Settings settings;
```

### 4.3 Parameter on Setter Methods

```java
@Parameter(name = "url", property = "url")
private String url;

private int timeout;
private String option0;
private String option1;

@Parameter(property = "timeout")
public void setTimeout(int timeout) {
    this.timeout = timeout;
}

@Parameter(property = "options")
public void setOptions(String[] options) {
    this.option0 = options[0];
    this.option1 = options[1];
}
```

### 4.4 Complex Parameter Types

```java
// List parameter
@Parameter
private List<String> includes;

// Map parameter
@Parameter
private Map<String, String> properties;

// Nested object parameter
@Parameter
private ServerConfig serverConfig;

public class ServerConfig {
    private String host;
    private int port;
    // getters/setters
}
```

**POM Configuration:**

```xml
<configuration>
    <includes>
        <include>**/*.java</include>
        <include>**/*.xml</include>
    </includes>
    <properties>
        <key1>value1</key1>
        <key2>value2</key2>
    </properties>
    <serverConfig>
        <host>localhost</host>
        <port>8080</port>
    </serverConfig>
</configuration>
```

## 5. Error Handling Best Practices

### 5.1 MojoExecutionException vs MojoFailureException

| Exception | When to Use | Build Result |
|-----------|-------------|--------------|
| `MojoExecutionException` | Unexpected problems (bugs, configuration errors) | BUILD ERROR |
| `MojoFailureException` | Expected failures (validation failed, tests failed) | BUILD FAILURE |

```java
@Override
public void execute() throws MojoExecutionException, MojoFailureException {
    
    // Configuration/unexpected error - use MojoExecutionException
    if (outputDirectory == null) {
        throw new MojoExecutionException("Output directory not configured");
    }
    
    try {
        processFiles();
    } catch (IOException e) {
        throw new MojoExecutionException("Error processing files", e);
    }
    
    // Expected failure condition - use MojoFailureException
    if (validationErrors > 0) {
        throw new MojoFailureException("Validation failed with " + validationErrors + " errors");
    }
}
```

### 5.2 Logging Best Practices

```java
@Override
public void execute() throws MojoExecutionException {
    // Use getLog() from AbstractMojo
    getLog().debug("Debug message - only shown with -X flag");
    getLog().info("Info message - normal output");
    getLog().warn("Warning message - potential issues");
    getLog().error("Error message - problems occurred");
    
    // With exceptions
    try {
        // ...
    } catch (Exception e) {
        getLog().error("Operation failed", e);
    }
}
```

### 5.3 Avoid System Properties

```java
// BAD - breaks embedded Maven usage
String value = System.getProperty("maven.test.skip");

// GOOD - use session properties
@Parameter(defaultValue = "${session}", readonly = true)
private MavenSession session;

public void execute() {
    String value = session.getUserProperties().getProperty("maven.test.skip");
    // Or from execution properties
    String value2 = session.getExecutionProperties().getProperty("maven.test.skip");
}
```

## 6. Common Bugs and Pitfalls

### 6.1 Resolving Relative Paths

```java
// BAD - resolves against working directory
File file = new File(path).getAbsoluteFile();

// GOOD - resolve against project base directory
File file = new File(path);
if (!file.isAbsolute()) {
    file = new File(project.getBasedir(), path);
}

// BETTER - use File parameter type (auto-resolved)
@Parameter(property = "outputDir", defaultValue = "${project.build.directory}/output")
private File outputDirectory;  // Automatically resolved correctly
```

### 6.2 File Encoding

```java
// BAD - uses platform default encoding
Reader reader = new FileReader(javaFile);
Writer writer = new FileWriter(xmlFile);

// GOOD - explicit encoding
Reader reader = new InputStreamReader(
    new FileInputStream(javaFile), StandardCharsets.UTF_8);

Writer writer = new OutputStreamWriter(
    new FileOutputStream(xmlFile), StandardCharsets.UTF_8);

// For XML files - use Plexus utilities
Reader xmlReader = ReaderFactory.newXmlReader(xmlFile);
Writer xmlWriter = WriterFactory.newXmlWriter(xmlFile);
```

### 6.3 URL to File Path Conversion

```java
// BAD - getPath() returns encoded URL path
URL url = getClass().getResource("/config.xml");
File path = new File(url.getPath());  // Breaks with spaces!

// BAD - URLDecoder converts + to space incorrectly
File path = new File(URLDecoder.decode(url.getPath(), "UTF-8"));

// GOOD - use URI constructor
File path = new File(url.toURI());

// BEST - use Plexus/Commons IO utilities
File path = FileUtils.toFile(url);
```

### 6.4 Avoid Shutdown Hooks

```java
// BAD - keeps resources until JVM shutdown (IDE/CI issues)
File tempFile = File.createTempFile("temp", null);
tempFile.deleteOnExit();

// GOOD - explicit cleanup with try-finally
File tempFile = File.createTempFile("temp", null);
try {
    // use temp file
} finally {
    tempFile.delete();
}
```

### 6.5 Case-Insensitive String Comparison

```java
// BAD - locale-dependent
if ("info".equals(debugLevel.toLowerCase())) { }

// GOOD - locale-insensitive
if ("info".equalsIgnoreCase(debugLevel)) { }

// For to*Case - use explicit locale
String normalized = value.toLowerCase(Locale.ENGLISH);
```

## 7. Testing Approaches

### 7.1 Unit Testing with JUnit and Mockito

```java
@ExtendWith(MockitoExtension.class)
class GreetingMojoTest {
    
    @Mock
    private MavenProject mavenProject;
    
    @InjectMocks
    private GreetingMojo mojo;
    
    @Test
    void testExecute() throws MojoExecutionException {
        mojo.execute();
        // Verify interactions as needed
    }
}
```

### 7.2 Maven Plugin Testing Harness

```xml
<dependency>
    <groupId>org.apache.maven.plugin-testing</groupId>
    <artifactId>maven-plugin-testing-harness</artifactId>
    <version>3.4.0</version>
    <scope>test</scope>
</dependency>
```

```java
@MojoTest
class YourMojoTest {
    
    @Inject
    private MavenProject mavenProject;
    
    @Provides
    private MavenProject project() {
        return mock(MavenProject.class);
    }
    
    @Test
    @InjectMojo(goal = "yourGoal", pom = "src/test/resources/unit/basic-test/pom.xml")
    @MojoParameter(name = "parameter1", value = "value1")
    void testMojoGoal(YourMojo mojo) throws Exception {
        when(mavenProject.getVersion()).thenReturn("1.0.0");
        
        mojo.execute();
        
        verify(mavenProject).getVersion();
    }
}
```

### 7.3 Integration Testing with maven-invoker-plugin

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-invoker-plugin</artifactId>
    <version>3.9.1</version>
    <configuration>
        <projectsDirectory>src/it</projectsDirectory>
        <pomIncludes>
            <pomInclude>**/pom.xml</pomInclude>
        </pomIncludes>
        <postBuildHookScript>verify</postBuildHookScript>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>run</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### 7.4 Integration Testing with maven-verifier

```java
import org.apache.maven.shared.verifier.Verifier;
import org.apache.maven.shared.verifier.VerificationException;

@Test
void testPluginGoal() throws VerificationException {
    File testDir = ResourceExtractor.simpleExtractResources(
        getClass(), "/projects/basic-test");
    
    Verifier verifier = new Verifier(testDir.getAbsolutePath());
    verifier.executeGoal("package");
    verifier.verifyErrorFreeLog();
    verifier.verifyFilePresent("target/output.txt");
}
```

## 8. Project Setup

### 8.1 Plugin POM Structure

```xml
<project>
    <modelVersion>4.0.0</modelVersion>
    
    <groupId>sample.plugin</groupId>
    <artifactId>hello-maven-plugin</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>maven-plugin</packaging>
    
    <properties>
        <maven-plugin-tools.version>3.15.2</maven-plugin-tools.version>
    </properties>
    
    <dependencies>
        <dependency>
            <groupId>org.apache.maven</groupId>
            <artifactId>maven-plugin-api</artifactId>
            <version>3.9.9</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.apache.maven.plugin-tools</groupId>
            <artifactId>maven-plugin-annotations</artifactId>
            <version>${maven-plugin-tools.version}</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>
    
    <build>
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-plugin-plugin</artifactId>
                    <version>${maven-plugin-tools.version}</version>
                    <executions>
                        <execution>
                            <id>help-mojo</id>
                            <goals>
                                <goal>helpmojo</goal>
                            </goals>
                        </execution>
                    </executions>
                </plugin>
            </plugins>
        </pluginManagement>
    </build>
    
    <reporting>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-plugin-report-plugin</artifactId>
                <version>${maven-plugin-tools.version}</version>
            </plugin>
        </plugins>
    </reporting>
</project>
```

### 8.2 Plugin Naming Convention

- Name format: `<yourplugin>-maven-plugin`
- **Avoid**: `maven-<yourplugin>-plugin` (reserved for Apache Maven plugins)
- Goal prefix derived from artifact ID: `hello-maven-plugin` → `hello:sayhi`

### 8.3 Lifecycle Bindings for maven-plugin Packaging

| Phase | Goal |
|-------|------|
| compile | Compiles Java code |
| process-classes | Extracts plugin descriptor |
| test | Runs unit tests |
| package | Creates plugin JAR |
| install | Installs to local repository |
| deploy | Deploys to remote repository |

## 9. References

- [Maven Plugin API](https://maven.apache.org/ref/current/maven-plugin-api/apidocs/index.html)
- [Mojo API Specification](https://maven.apache.org/developers/mojo-api-specification.html)
- [Maven Plugin Tools](https://maven.apache.org/plugin-tools/)
- [Maven Plugin Testing Harness](https://maven.apache.org/plugin-testing/maven-plugin-testing-harness/)
- [Plugin Parameter Expression Evaluator](https://maven.apache.org/ref/current/maven-core/apidocs/org/apache/maven/plugin/PluginParameterExpressionEvaluator.html)
- [Eclipse Sisu](https://eclipse.dev/sisu/index.html) - IoC container used by Maven 3
- [JSR-330 in Maven](https://maven.apache.org/maven-jsr330.html)
- [Common Bugs and Pitfalls](https://maven.apache.org/plugin-developers/common-bugs.html)
