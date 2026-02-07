# Tomcat Versions

Compatibility guide for Apache Tomcat versions supported by this plugin.

## Supported Versions

| Tomcat | Jakarta EE | Java | Servlet | JSP | EL | WebSocket |
|--------|------------|------|---------|-----|-----|-----------|
| **10.1.x** | 10 | 11+ | 6.0 | 3.1 | 5.0 | 2.1 |
| **11.x** | 11 | 17+ | 6.1 | 4.0 | 6.0 | 2.2 |

## Version Configuration

### Specific Version

```xml
<configuration>
    <tomcatVersion>10.1.52</tomcatVersion>
</configuration>
```

Or via command line:

```bash
mvn tomcat:run -Dtomcat.version=10.1.52
```

### Latest Stable Versions (as of 2026)

| Branch | Latest Version |
|--------|----------------|
| Tomcat 10.1.x | `10.1.52` |
| Tomcat 11.x | `11.0.4` |

## Tomcat 10.1.x (Jakarta EE 10)

### Features

- Jakarta EE 10 specification
- `jakarta.*` namespace (migrated from `javax.*`)
- Servlet 6.0
- JSP 3.1
- Expression Language 5.0
- WebSocket 2.1

### Minimum Requirements

- Java 11 (recommended: Java 17 or 21)
- Maven 3.9.6+

### When to Use

- Production applications targeting Jakarta EE 10
- Applications migrated from Tomcat 9.x
- Maximum stability and LTS support

### Configuration Example

```xml
<plugin>
    <groupId>io.github.rajendarreddyj</groupId>
    <artifactId>tomcat-maven-plugin</artifactId>
    <version>1.0.0</version>
    <configuration>
        <tomcatVersion>10.1.52</tomcatVersion>
    </configuration>
</plugin>
```

## Tomcat 11.x (Jakarta EE 11)

### Features

- Jakarta EE 11 specification
- Latest `jakarta.*` namespace
- Servlet 6.1
- JSP 4.0
- Expression Language 6.0
- WebSocket 2.2
- HTTP/2 improvements
- Virtual threads support (Java 21+)

### Minimum Requirements

- Java 17 (recommended: Java 21 for virtual threads)
- Maven 3.9.6+

### When to Use

- New applications leveraging Jakarta EE 11
- Applications requiring latest features
- Development environments testing future compatibility

### Configuration Example

```xml
<plugin>
    <groupId>io.github.rajendarreddyj</groupId>
    <artifactId>tomcat-maven-plugin</artifactId>
    <version>1.0.0</version>
    <configuration>
        <tomcatVersion>11.0.4</tomcatVersion>
    </configuration>
</plugin>
```

## Migration from javax.* to jakarta.*

### Background

- **Tomcat 9.x and earlier**: Uses `javax.servlet.*`, `javax.websocket.*`, etc.
- **Tomcat 10.x and later**: Uses `jakarta.servlet.*`, `jakarta.websocket.*`, etc.

### Required Changes

**Imports:**

```java
// Before (Tomcat 9.x)
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

// After (Tomcat 10.1.x / 11.x)
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
```

**Dependencies:**

```xml
<!-- Before (Tomcat 9.x) -->
<dependency>
    <groupId>javax.servlet</groupId>
    <artifactId>javax.servlet-api</artifactId>
    <version>4.0.1</version>
    <scope>provided</scope>
</dependency>

<!-- After (Tomcat 10.1.x / 11.x) -->
<dependency>
    <groupId>jakarta.servlet</groupId>
    <artifactId>jakarta.servlet-api</artifactId>
    <version>6.0.0</version>
    <scope>provided</scope>
</dependency>
```

### Automated Migration Tools

- **Apache Tomcat Migration Tool**: Transforms bytecode from `javax.*` to `jakarta.*`
- **Eclipse Transformer**: Similar bytecode transformation
- **IDE refactoring**: IntelliJ and Eclipse have migration assists

## Auto-Download

When `catalinaHome` is not specified, the plugin downloads Tomcat automatically:

### Download Sources

1. **Primary**: Apache CDN (`dlcdn.apache.org`)
2. **Fallback**: Apache Archive (`archive.apache.org`)

### Download Location

```
~/.m2/tomcat-cache/apache-tomcat-{version}/
```

Configurable via:

```xml
<configuration>
    <tomcatCacheDir>/custom/cache/dir</tomcatCacheDir>
</configuration>
```

### Checksum Verification

Downloads are verified using SHA-512 checksums to ensure integrity.

## Using Existing Tomcat Installation

If you have Tomcat installed, specify the path:

```xml
<configuration>
    <catalinaHome>/opt/tomcat</catalinaHome>
</configuration>
```

Or:

```bash
mvn tomcat:run -Dtomcat.catalina.home=/opt/tomcat
```

**Note**: The plugin will read the version from the existing installation.

## Version Detection

The plugin detects Tomcat version from:

1. `catalinaHome/lib/catalina.jar` → `ServerInfo.properties`
2. Property: `server.number` (e.g., `10.1.52.0`)

## See Also

- [Getting Started](Getting-Started) - Quick setup
- [Configuration](Configuration) - All parameters
- [Troubleshooting](Troubleshooting) - Common issues
