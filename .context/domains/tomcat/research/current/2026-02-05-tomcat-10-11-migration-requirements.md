---
title: Tomcat 10.1 and 11 Migration Requirements and Download URLs
date: 2026-02-05
author: GitHub Copilot
sources:
  - https://tomcat.apache.org/migration-10.1.html
  - https://tomcat.apache.org/migration-11.0.html
  - https://tomcat.apache.org/download-10.cgi
  - https://tomcat.apache.org/download-11.cgi
  - https://tomcat.apache.org/whichversion.html
---

# Tomcat 10.1 and 11 Migration Requirements

## Java Version Compatibility Matrix

| Tomcat Version | Minimum Java Version | Jakarta EE Platform |
|----------------|---------------------|---------------------|
| 11.0.x         | Java 17             | Jakarta EE 11       |
| 10.1.x         | Java 11             | Jakarta EE 10       |
| 10.0.x (EOL)   | Java 8              | Jakarta EE 9        |
| 9.0.x          | Java 8              | Java EE 8           |

## Tomcat 10.1.x Requirements and Features

### Java Requirement
- **Minimum Java Version:** Java 11 or later
- Change from Tomcat 10.0.x and 9.0.x which required Java 8 or later

### Specification APIs (Jakarta EE 10)

| Specification | Version |
|---------------|---------|
| Servlet       | 6.0     |
| Pages (JSP)   | 3.1     |
| Expression Language | 5.0 |
| WebSocket     | 2.1     |
| Authentication (JASPIC) | 3.0 |
| Annotation    | 2.1     |
| JDSOL         | 2.0     |

### Key Changes from Tomcat 10.0.x

#### Servlet 6.0 API
- All deprecated methods/classes from 5.0 API removed
- Cookie specifications other than RFC 6265 removed
- New method: `Cookie.setAttribute(String name, String value)`
- URI decoding and normalizing process clarified
- New methods/classes for unique identifiers for requests/connections

#### Pages 3.1
- Option to raise `PropertyNotFoundException` when EL expression contains unknown identifier

#### Expression Language 5.0
- EL API now uses generics where appropriate
- Deprecated `MethodExpression.isParmetersProvided()` method removed

#### WebSocket 2.1
- API JAR packaging changed to remove duplicate classes
- Server API now depends on client API JAR

#### Authentication 3.0
- No changes from previous version

#### Internal APIs
- Not binary compatible with Tomcat 10.0
- All deprecated code from 10.0.x removed
- Configuration options removed from `JreMemoryLeakPreventionListener` (leaks don't exist in Java 11+)

### Notable 10.1.x Changes
- **10.1.8+:** Default `maxParameterCount` reduced from 10,000 to 1,000
- **10.1.0-M5:** APR connector removed (was deprecated in Tomcat 10.0)
- **10.1.0-M3:** No longer adds "Expires" header with "Cache-Control: private"
- **10.1.45:** Avoid this version if using FileStore session mechanism (packaging error)

---

## Tomcat 11.x Requirements and Features

### Java Requirement
- **Minimum Java Version:** Java 17 or later
- SecurityManager support removed (deprecated in Java 17+)

### Specification APIs (Jakarta EE 11)

| Specification | Version |
|---------------|---------|
| Servlet       | 6.1     |
| Pages (JSP)   | 4.0     |
| Expression Language | 6.0 |
| WebSocket     | 2.2     |
| Authentication (JASPIC) | 3.1 |
| Annotation    | 3.0     |
| JDSOL         | 2.0     |

### Key Changes from Tomcat 10.1.x

#### Servlet 6.1 API
- HTTP header behavior with `null`/empty string clarified
- `ServletOutputStream.isReady()` "write operation" meaning clarified
- `ServletRequest.getParameter()` documented to throw runtime exception on parsing errors
- HTTP/2 server push support removed (optional in spec)
- `Cookie.setAttribute()`/`getAttribute()` behavior clarified for HttpOnly, Secure, Partitioned

#### Pages 4.0
- All deprecated methods/classes from 3.1 API removed

#### Expression Language 6.0
- All deprecated methods/classes from 5.0 API removed
- SecurityManager support removed

#### WebSocket 2.2
- SecurityManager support removed

#### Authentication 3.1
- SecurityManager support removed

#### Annotations 3.0
- No changes

#### Internal APIs
- Not binary compatible with Tomcat 10.1
- All deprecated code from 10.1.x removed
- Byte-to-character conversions now throw exception on failure
- WAR URL handler ^ separator removed (must use *)
- Cookie parsing treats quotes in quoted values as part of value (RFC 6265)

### Notable 11.0.x Changes
- **11.0.0-M5+:** Default `maxParameterCount` reduced from 10,000 to 1,000

---

## Supported Platforms

Both Tomcat 10.1.x and 11.x support:
- **Linux/Unix:** tar.gz distribution
- **Windows 64-bit:** zip distribution, Service Installer (.exe)
- **Windows 32-bit:** zip distribution (10.1.x only - 11.x is 64-bit only)
- **macOS:** tar.gz distribution
- Any platform with supported Java version

---

## Download URLs and Patterns

### Tomcat 10.1.x (Current Version: 10.1.52)

#### Base URL Pattern
```
https://dlcdn.apache.org/tomcat/tomcat-10/v{VERSION}/bin/
```

#### Binary Distribution URLs

| Type | URL |
|------|-----|
| Core zip | `https://dlcdn.apache.org/tomcat/tomcat-10/v10.1.52/bin/apache-tomcat-10.1.52.zip` |
| Core tar.gz | `https://dlcdn.apache.org/tomcat/tomcat-10/v10.1.52/bin/apache-tomcat-10.1.52.tar.gz` |
| Windows 32-bit zip | `https://dlcdn.apache.org/tomcat/tomcat-10/v10.1.52/bin/apache-tomcat-10.1.52-windows-x86.zip` |
| Windows 64-bit zip | `https://dlcdn.apache.org/tomcat/tomcat-10/v10.1.52/bin/apache-tomcat-10.1.52-windows-x64.zip` |
| Windows Service Installer | `https://dlcdn.apache.org/tomcat/tomcat-10/v10.1.52/bin/apache-tomcat-10.1.52.exe` |
| Full Documentation | `https://dlcdn.apache.org/tomcat/tomcat-10/v10.1.52/bin/apache-tomcat-10.1.52-fulldocs.tar.gz` |
| Deployer zip | `https://dlcdn.apache.org/tomcat/tomcat-10/v10.1.52/bin/apache-tomcat-10.1.52-deployer.zip` |
| Deployer tar.gz | `https://dlcdn.apache.org/tomcat/tomcat-10/v10.1.52/bin/apache-tomcat-10.1.52-deployer.tar.gz` |
| Embedded tar.gz | `https://dlcdn.apache.org/tomcat/tomcat-10/v10.1.52/bin/embed/apache-tomcat-10.1.52-embed.tar.gz` |
| Embedded zip | `https://dlcdn.apache.org/tomcat/tomcat-10/v10.1.52/bin/embed/apache-tomcat-10.1.52-embed.zip` |

#### Source Distribution URLs
| Type | URL |
|------|-----|
| Source tar.gz | `https://dlcdn.apache.org/tomcat/tomcat-10/v10.1.52/src/apache-tomcat-10.1.52-src.tar.gz` |
| Source zip | `https://dlcdn.apache.org/tomcat/tomcat-10/v10.1.52/src/apache-tomcat-10.1.52-src.zip` |

#### Archive/Browse URLs
- Browse: `https://dlcdn.apache.org/tomcat/tomcat-10`
- Archives: `https://archive.apache.org/dist/tomcat/tomcat-10`
- KEYS: `https://downloads.apache.org/tomcat/tomcat-10/KEYS`

---

### Tomcat 11.x (Current Version: 11.0.18)

#### Base URL Pattern
```
https://dlcdn.apache.org/tomcat/tomcat-11/v{VERSION}/bin/
```

#### Binary Distribution URLs

| Type | URL |
|------|-----|
| Core zip | `https://dlcdn.apache.org/tomcat/tomcat-11/v11.0.18/bin/apache-tomcat-11.0.18.zip` |
| Core tar.gz | `https://dlcdn.apache.org/tomcat/tomcat-11/v11.0.18/bin/apache-tomcat-11.0.18.tar.gz` |
| Windows zip | `https://dlcdn.apache.org/tomcat/tomcat-11/v11.0.18/bin/apache-tomcat-11.0.18-windows-x64.zip` |
| Windows Service Installer | `https://dlcdn.apache.org/tomcat/tomcat-11/v11.0.18/bin/apache-tomcat-11.0.18.exe` |
| Full Documentation | `https://dlcdn.apache.org/tomcat/tomcat-11/v11.0.18/bin/apache-tomcat-11.0.18-fulldocs.tar.gz` |
| Deployer zip | `https://dlcdn.apache.org/tomcat/tomcat-11/v11.0.18/bin/apache-tomcat-11.0.18-deployer.zip` |
| Deployer tar.gz | `https://dlcdn.apache.org/tomcat/tomcat-11/v11.0.18/bin/apache-tomcat-11.0.18-deployer.tar.gz` |
| Embedded tar.gz | `https://dlcdn.apache.org/tomcat/tomcat-11/v11.0.18/bin/embed/apache-tomcat-11.0.18-embed.tar.gz` |
| Embedded zip | `https://dlcdn.apache.org/tomcat/tomcat-11/v11.0.18/bin/embed/apache-tomcat-11.0.18-embed.zip` |

#### Source Distribution URLs
| Type | URL |
|------|-----|
| Source tar.gz | `https://dlcdn.apache.org/tomcat/tomcat-11/v11.0.18/src/apache-tomcat-11.0.18-src.tar.gz` |
| Source zip | `https://dlcdn.apache.org/tomcat/tomcat-11/v11.0.18/src/apache-tomcat-11.0.18-src.zip` |

#### Archive/Browse URLs
- Browse: `https://dlcdn.apache.org/tomcat/tomcat-11`
- Archives: `https://archive.apache.org/dist/tomcat/tomcat-11`
- KEYS: `https://downloads.apache.org/tomcat/tomcat-11/KEYS`

---

## URL Pattern Templates

### Generic Download URL Patterns

```
# Core binary - zip
https://dlcdn.apache.org/tomcat/tomcat-{MAJOR}/v{VERSION}/bin/apache-tomcat-{VERSION}.zip

# Core binary - tar.gz
https://dlcdn.apache.org/tomcat/tomcat-{MAJOR}/v{VERSION}/bin/apache-tomcat-{VERSION}.tar.gz

# Windows 64-bit
https://dlcdn.apache.org/tomcat/tomcat-{MAJOR}/v{VERSION}/bin/apache-tomcat-{VERSION}-windows-x64.zip

# Windows 32-bit (10.1.x only)
https://dlcdn.apache.org/tomcat/tomcat-{MAJOR}/v{VERSION}/bin/apache-tomcat-{VERSION}-windows-x86.zip

# Windows Service Installer
https://dlcdn.apache.org/tomcat/tomcat-{MAJOR}/v{VERSION}/bin/apache-tomcat-{VERSION}.exe

# Embedded - tar.gz
https://dlcdn.apache.org/tomcat/tomcat-{MAJOR}/v{VERSION}/bin/embed/apache-tomcat-{VERSION}-embed.tar.gz

# Embedded - zip
https://dlcdn.apache.org/tomcat/tomcat-{MAJOR}/v{VERSION}/bin/embed/apache-tomcat-{VERSION}-embed.zip

# Deployer - zip
https://dlcdn.apache.org/tomcat/tomcat-{MAJOR}/v{VERSION}/bin/apache-tomcat-{VERSION}-deployer.zip

# Source - tar.gz
https://dlcdn.apache.org/tomcat/tomcat-{MAJOR}/v{VERSION}/src/apache-tomcat-{VERSION}-src.tar.gz
```

### Verification Files
```
# PGP Signature
{DOWNLOAD_URL}.asc

# SHA-512 Checksum
{DOWNLOAD_URL}.sha512
```

---

## Migration Considerations

### javax.* to jakarta.* Namespace Change
- All Jakarta EE APIs moved from `javax.*` to `jakarta.*` package namespace
- Affects Tomcat 10.0+ (not specific to 10.1 or 11)
- Migration tool available: https://github.com/apache/tomcat-jakartaee-migration

### Key Differences Summary

| Feature | Tomcat 10.1.x | Tomcat 11.x |
|---------|---------------|-------------|
| Min Java | 11 | 17 |
| Servlet | 6.0 | 6.1 |
| JSP | 3.1 | 4.0 |
| EL | 5.0 | 6.0 |
| WebSocket | 2.1 | 2.2 |
| Authentication | 3.0 | 3.1 |
| SecurityManager | Supported | Removed |
| HTTP/2 Push | Supported | Removed |
| Windows 32-bit | Supported | Not supported |
