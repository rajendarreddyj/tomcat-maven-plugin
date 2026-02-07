# FAQ

Frequently asked questions about the Tomcat Maven Plugin.

## General

### What Tomcat versions are supported?

The plugin supports:
- **Tomcat 10.1.x** (Jakarta EE 10, Java 11+)
- **Tomcat 11.x** (Jakarta EE 11, Java 17+)

See [Tomcat Versions](Tomcat-Versions) for details.

### Does this plugin support Tomcat 9.x or earlier?

No. This plugin is designed for modern Tomcat versions (10.1.x and 11.x) that use the Jakarta EE namespace (`jakarta.*`). For older Tomcat versions, use the legacy [Apache Tomcat Maven Plugin](https://tomcat.apache.org/maven-plugin.html).

### Do I need to install Tomcat?

No. The plugin automatically downloads Tomcat if `catalinaHome` is not specified. Downloads are cached in `~/.m2/tomcat-cache/`.

### Where is Tomcat downloaded to?

Default: `~/.m2/tomcat-cache/apache-tomcat-{version}/`

Configure with:
```xml
<configuration>
    <tomcatCacheDir>/custom/path</tomcatCacheDir>
</configuration>
```

## Configuration

### How do I change the port?

Via command line:
```bash
mvn tomcat:run -Dtomcat.http.port=9080
```

Via pom.xml:
```xml
<configuration>
    <httpPort>9080</httpPort>
</configuration>
```

### How do I deploy as ROOT (context path /)?

```xml
<configuration>
    <contextPath>/</contextPath>
</configuration>
```

Or:
```bash
mvn tomcat:run -Dtomcat.context.path=/
```

### How do I add JVM options?

```xml
<configuration>
    <vmOptions>
        <vmOption>-Xmx1024m</vmOption>
        <vmOption>-Dmy.property=value</vmOption>
    </vmOptions>
</configuration>
```

### How do I set environment variables?

```xml
<configuration>
    <environmentVariables>
        <MY_VAR>my_value</MY_VAR>
        <JAVA_OPTS>-Dfile.encoding=UTF-8</JAVA_OPTS>
    </environmentVariables>
</configuration>
```

### How do I skip plugin execution?

Via command line:
```bash
mvn package -Dtomcat.skip=true
```

Via pom.xml:
```xml
<configuration>
    <skip>true</skip>
</configuration>
```

## Deployment

### How do I enable hot deployment?

```bash
mvn tomcat:run -Dtomcat.autopublish.enabled=true
```

Or in pom.xml:
```xml
<configuration>
    <autopublishEnabled>true</autopublishEnabled>
</configuration>
```

See [Hot Deployment](Hot-Deployment) for details.

### How do I manually redeploy?

With background mode:
```bash
mvn tomcat:start       # Start in background
mvn compile tomcat:deploy  # Redeploy after changes
mvn tomcat:stop        # Stop when done
```

### Where should my WAR content be?

Default: `target/${project.build.finalName}/` (exploded WAR)

Configure with:
```xml
<configuration>
    <warSourceDirectory>${project.build.directory}/my-webapp</warSourceDirectory>
</configuration>
```

## Debugging

### How do I debug my application?

```bash
mvn tomcat:debug
```

Then attach your IDE debugger to `localhost:5005`.

See [Debugging](Debugging) for IDE setup guides.

### How do I change the debug port?

```bash
mvn tomcat:debug -Dtomcat.debug.port=8000
```

### How do I debug startup issues?

Use suspend mode to pause until debugger attaches:
```bash
mvn tomcat:debug -Dtomcat.debug.suspend=true
```

## Multiple Instances

### Can I run multiple Tomcat instances?

Yes. Use different ports:

```bash
# Terminal 1
mvn tomcat:run -Dtomcat.http.port=8080

# Terminal 2 (different project or same project in different directory)
mvn tomcat:run -Dtomcat.http.port=9080
```

### How does CATALINA_BASE work?

When using a non-default port, the plugin auto-generates a separate CATALINA_BASE at:
```
~/.m2/tomcat-cache/base-{version}-{port}/
```

This allows multiple instances sharing the same CATALINA_HOME.

## Migration

### I'm migrating from Tomcat 9.x. What do I need to change?

1. **Change imports** from `javax.*` to `jakarta.*`
2. **Update dependencies** to Jakarta EE versions
3. **Update web.xml** namespace if using schema validation

See [Tomcat Versions](Tomcat-Versions#migration-from-javax-to-jakarta).

### Is this plugin compatible with the Apache Tomcat Maven Plugin?

This is a separate plugin with similar functionality but modernized for Tomcat 10.1.x/11.x. Configuration parameters are similar but not identical.

## Performance

### How do I improve startup time?

1. **Use local Tomcat** instead of downloading:
   ```xml
   <configuration>
       <catalinaHome>/path/to/tomcat</catalinaHome>
   </configuration>
   ```

2. **Reduce scan paths** in your application

3. **Use parallel initialization** in your application

### How do I increase memory?

```xml
<configuration>
    <vmOptions>
        <vmOption>-Xms512m</vmOption>
        <vmOption>-Xmx2048m</vmOption>
    </vmOptions>
</configuration>
```

## Security

### Is debug mode secure?

No. Debug mode opens a network port that allows remote code execution. Only use in development environments and consider restricting to localhost:

```bash
mvn tomcat:debug -Dtomcat.debug.host=localhost
```

### Should I use this plugin in production?

This plugin is designed for **development and testing**. For production:
- Deploy WAR files directly to Tomcat
- Use container orchestration (Docker, Kubernetes)
- Use proper deployment automation

## Still Have Questions?

- Check [Troubleshooting](Troubleshooting) for common issues
- Search [GitHub Issues](https://github.com/rajendarreddyj/tomcat-maven-plugin/issues)
- Open a new issue with your question
