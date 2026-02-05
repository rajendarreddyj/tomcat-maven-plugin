# Architecture Documentation

This document describes the architecture of the Maven Tomcat Plugin, including component diagrams, class relationships, and sequence flows.

## Table of Contents

- [Overview](#overview)
- [Component Architecture](#component-architecture)
- [Package Structure](#package-structure)
- [Class Diagrams](#class-diagrams)
- [Sequence Diagrams](#sequence-diagrams)
  - [tomcat:run Goal](#tomcatrun-goal)
  - [tomcat:start Goal](#tomcatstart-goal)
  - [tomcat:stop Goal](#tomcatstop-goal)
  - [tomcat:deploy Goal](#tomcatdeploy-goal)
  - [Tomcat Download Flow](#tomcat-download-flow)
  - [Hot Deployment Flow](#hot-deployment-flow)
- [Configuration Flow](#configuration-flow)
- [Design Patterns](#design-patterns)

---

## Overview

The Maven Tomcat Plugin is organized into four primary packages:

| Package | Responsibility |
|---------|----------------|
| `io.github.rajendarreddyj.tomcat` | Maven Mojos (plugin goals) |
| `io.github.rajendarreddyj.tomcat.config` | Configuration classes and builders |
| `io.github.rajendarreddyj.tomcat.download` | Tomcat download and validation |
| `io.github.rajendarreddyj.tomcat.deploy` | WAR deployment and hot-reload |
| `io.github.rajendarreddyj.tomcat.lifecycle` | Tomcat process management |

---

## Component Architecture

```mermaid
graph TB
    subgraph "Maven Plugin Layer"
        RunMojo[RunMojo<br/>tomcat:run]
        StartMojo[StartMojo<br/>tomcat:start]
        StopMojo[StopMojo<br/>tomcat:stop]
        DeployMojo[DeployMojo<br/>tomcat:deploy]
        AbstractMojo[AbstractTomcatMojo]
    end

    subgraph "Configuration Layer"
        ServerConfig[ServerConfiguration]
        DeployConfig[DeployableConfiguration]
        TomcatVersion[TomcatVersion]
        CatalinaBaseGen[CatalinaBaseGenerator]
    end

    subgraph "Download Layer"
        Downloader[TomcatDownloader]
        Validator[ChecksumValidator]
    end

    subgraph "Deployment Layer"
        Deployer[ExplodedWarDeployer]
        Watcher[HotDeployWatcher]
    end

    subgraph "Lifecycle Layer"
        Launcher[TomcatLauncher]
    end

    subgraph "External"
        Maven[Maven Runtime]
        Tomcat[Apache Tomcat]
        FS[File System]
        Net[Network/Apache CDN]
    end

    RunMojo --> AbstractMojo
    StartMojo --> AbstractMojo
    StopMojo --> AbstractMojo
    DeployMojo --> AbstractMojo

    AbstractMojo --> ServerConfig
    AbstractMojo --> DeployConfig
    AbstractMojo --> TomcatVersion
    AbstractMojo --> Downloader
    AbstractMojo --> CatalinaBaseGen

    RunMojo --> Deployer
    RunMojo --> Watcher
    RunMojo --> Launcher

    StartMojo --> Deployer
    StartMojo --> Launcher

    StopMojo --> Launcher

    DeployMojo --> Deployer

    Downloader --> Validator
    Downloader --> TomcatVersion
    Downloader --> Net

    Watcher --> Deployer
    Watcher --> FS

    Launcher --> Tomcat
    Deployer --> FS

    Maven --> RunMojo
    Maven --> StartMojo
    Maven --> StopMojo
    Maven --> DeployMojo
```

---

## Package Structure

```mermaid
graph LR
    subgraph "io.github.rajendarreddyj.tomcat"
        A1[AbstractTomcatMojo]
        A2[RunMojo]
        A3[StartMojo]
        A4[StopMojo]
        A5[DeployMojo]
    end

    subgraph "config"
        B1[ServerConfiguration]
        B2[DeployableConfiguration]
        B3[TomcatVersion]
        B4[CatalinaBaseGenerator]
    end

    subgraph "download"
        C1[TomcatDownloader]
        C2[ChecksumValidator]
    end

    subgraph "deploy"
        D1[ExplodedWarDeployer]
        D2[HotDeployWatcher]
    end

    subgraph "lifecycle"
        E1[TomcatLauncher]
    end

    A1 --> B1
    A1 --> B2
    A1 --> B3
    A1 --> B4
    A1 --> C1

    A2 --> E1
    A2 --> D1
    A2 --> D2

    A3 --> E1
    A3 --> D1

    A4 --> E1

    A5 --> D1

    C1 --> C2
    C1 --> B3

    D2 --> D1
```

---

## Class Diagrams

### Mojo Hierarchy

```mermaid
classDiagram
    class AbstractMojo {
        <<Maven API>>
        +execute()
        +getLog()
    }

    class AbstractTomcatMojo {
        #String tomcatVersion
        #File catalinaHome
        #File catalinaBase
        #File tomcatCacheDir
        #int httpPort
        #String httpHost
        #long startupTimeout
        #long shutdownTimeout
        #boolean skip
        #File javaHome
        #List~String~ vmOptions
        #Map~String,String~ environmentVariables
        #String contextPath
        #File warSourceDirectory
        #String deploymentOutputName
        #boolean autopublishEnabled
        #int autopublishInactivityLimit
        #List~String~ classpathAdditions
        #MavenProject project
        +resolveCatalinaHome() Path
        +downloadTomcat() Path
        +validateTomcatInstallation(Path)
        +validateJavaVersion()
        +validatePortAvailable()
        +buildServerConfiguration() ServerConfiguration
        +buildDeployableConfiguration() DeployableConfiguration
    }

    class RunMojo {
        +execute()
    }

    class StartMojo {
        +execute()
        -storePid(Path, Process)
    }

    class StopMojo {
        +execute()
        -stopProcess(long)
        -stopViaScript(ServerConfiguration)
    }

    class DeployMojo {
        +execute()
    }

    AbstractMojo <|-- AbstractTomcatMojo
    AbstractTomcatMojo <|-- RunMojo
    AbstractTomcatMojo <|-- StartMojo
    AbstractTomcatMojo <|-- StopMojo
    AbstractTomcatMojo <|-- DeployMojo
```

### Configuration Classes

```mermaid
classDiagram
    class ServerConfiguration {
        -Path catalinaHome
        -Path catalinaBase
        -String httpHost
        -int httpPort
        -Path javaHome
        -List~String~ vmOptions
        -Map~String,String~ environmentVariables
        -long startupTimeout
        -long shutdownTimeout
        -List~String~ classpathAdditions
        +getCatalinaHome() Path
        +getCatalinaBase() Path
        +getHttpHost() String
        +getHttpPort() int
        +getJavaHome() Path
        +getVmOptions() List
        +getEnvironmentVariables() Map
        +getStartupTimeout() long
        +getShutdownTimeout() long
        +getClasspathAdditions() List
        +builder()$ Builder
    }

    class ServerConfigurationBuilder {
        +catalinaHome(Path) Builder
        +catalinaBase(Path) Builder
        +httpHost(String) Builder
        +httpPort(int) Builder
        +javaHome(Path) Builder
        +vmOptions(List) Builder
        +environmentVariables(Map) Builder
        +startupTimeout(long) Builder
        +shutdownTimeout(long) Builder
        +classpathAdditions(List) Builder
        +build() ServerConfiguration
    }

    class DeployableConfiguration {
        -String moduleName
        -Path sourcePath
        -String contextPath
        -Path deployDir
        -boolean autopublishEnabled
        -int autopublishInactivityLimit
        -String deploymentOutputName
        +getModuleName() String
        +getSourcePath() Path
        +getContextPath() String
        +getDeployDir() Path
        +isAutopublishEnabled() boolean
        +getAutopublishInactivityLimit() int
        +getDeploymentOutputName() String
        +getTargetDirectoryName() String
        +builder()$ Builder
    }

    class TomcatVersion {
        <<enumeration>>
        TOMCAT_10_1
        TOMCAT_11
        -String majorMinor
        -int minimumJava
        -String downloadBaseUrl
        +getMajorMinor() String
        +getMinimumJava() int
        +getDownloadUrl(String) String
        +getArchiveDownloadUrl(String) String
        +getChecksumUrl(String) String
        +fromVersionString(String)$ TomcatVersion
        +validateJavaVersion()
    }

    ServerConfiguration *-- ServerConfigurationBuilder
    DeployableConfiguration *-- DeployableConfigurationBuilder
```

### Service Classes

```mermaid
classDiagram
    class TomcatDownloader {
        -ChecksumValidator checksumValidator
        -HttpClient httpClient
        +download(String, Path, Log) Path
        -downloadWithFallback(TomcatVersion, String, Path, Log)
        -downloadFile(String, Path)
        -validateChecksum(Path, TomcatVersion, String, Log) boolean
        -extract(Path, Path, Log)
        -setExecutablePermissions(Path, Log)
        -isValidTomcatDir(Path) boolean
    }

    class ChecksumValidator {
        +validate(Path, String) boolean
        +calculateChecksum(Path) String
        -fetchChecksum(String) String
    }

    class TomcatLauncher {
        -ServerConfiguration config
        -Log log
        -Process tomcatProcess
        +run()
        +start()
        +stop()
        +getProcess() Process
        -startProcess(String) Process
        -stopViaScript()
        -resolveCatalinaScript() Path
        -configureEnvironment(Map)
        -waitForStartup()
        -isServerReady() boolean
    }

    class ExplodedWarDeployer {
        -Log log
        +deploy(DeployableConfiguration)
        +redeploy(DeployableConfiguration)
        +syncChanges(DeployableConfiguration, Path)
        -copyDirectory(Path, Path)
        -deleteDirectory(Path)
    }

    class HotDeployWatcher {
        -DeployableConfiguration config
        -ExplodedWarDeployer deployer
        -Log log
        -ScheduledExecutorService scheduler
        -WatchService watchService
        +start()
        +close()
        -registerRecursive(Path)
        -watch()
        -checkAndSync()
        -performSync()
    }

    class CatalinaBaseGenerator {
        <<utility>>
        +generate(Path, Path, int, String)$
        +isValidCatalinaBase(Path)$ boolean
        -modifyServerXml(Path, int, String)$
    }

    TomcatDownloader --> ChecksumValidator
    TomcatDownloader --> TomcatVersion
    HotDeployWatcher --> ExplodedWarDeployer
```

---

## Sequence Diagrams

### tomcat:run Goal

```mermaid
sequenceDiagram
    participant User
    participant Maven
    participant RunMojo
    participant AbstractMojo as AbstractTomcatMojo
    participant Downloader as TomcatDownloader
    participant Deployer as ExplodedWarDeployer
    participant Watcher as HotDeployWatcher
    participant Launcher as TomcatLauncher
    participant Tomcat

    User->>Maven: mvn tomcat:run
    Maven->>RunMojo: execute()
    
    alt skip=true
        RunMojo-->>Maven: Skip execution
    end

    RunMojo->>AbstractMojo: validateJavaVersion()
    RunMojo->>AbstractMojo: validatePortAvailable()
    
    RunMojo->>AbstractMojo: buildServerConfiguration()
    AbstractMojo->>AbstractMojo: resolveCatalinaHome()
    
    alt catalinaHome not found
        AbstractMojo->>Downloader: download(version, cacheDir, log)
        Downloader-->>AbstractMojo: extractedPath
    end
    
    AbstractMojo-->>RunMojo: ServerConfiguration
    
    RunMojo->>AbstractMojo: buildDeployableConfiguration()
    AbstractMojo-->>RunMojo: DeployableConfiguration
    
    RunMojo->>Deployer: deploy(config)
    Deployer->>Deployer: copyDirectory(source, webapps)
    Deployer-->>RunMojo: deployed
    
    RunMojo->>Watcher: start()
    
    alt autopublish enabled
        Watcher->>Watcher: registerRecursive(sourcePath)
        Watcher->>Watcher: start watch thread
    end
    
    RunMojo->>Launcher: run()
    Launcher->>Launcher: startProcess("run")
    Launcher->>Tomcat: catalina.sh run
    Tomcat-->>Launcher: process started
    
    Note over Launcher,Tomcat: Blocks until Ctrl+C
    
    User->>Tomcat: Ctrl+C (SIGINT)
    Tomcat-->>Launcher: shutdown
    Launcher->>Watcher: close()
    Launcher-->>RunMojo: exit
    RunMojo-->>Maven: complete
```

### tomcat:start Goal

```mermaid
sequenceDiagram
    participant User
    participant Maven
    participant StartMojo
    participant AbstractMojo as AbstractTomcatMojo
    participant Deployer as ExplodedWarDeployer
    participant Launcher as TomcatLauncher
    participant Tomcat
    participant FS as File System

    User->>Maven: mvn tomcat:start
    Maven->>StartMojo: execute()
    
    StartMojo->>AbstractMojo: validateJavaVersion()
    StartMojo->>AbstractMojo: validatePortAvailable()
    StartMojo->>AbstractMojo: buildServerConfiguration()
    AbstractMojo-->>StartMojo: ServerConfiguration
    
    StartMojo->>AbstractMojo: buildDeployableConfiguration()
    AbstractMojo-->>StartMojo: DeployableConfiguration
    
    StartMojo->>Deployer: deploy(config)
    Deployer-->>StartMojo: deployed
    
    StartMojo->>Launcher: start()
    Launcher->>Launcher: startProcess("start")
    Launcher->>Tomcat: catalina.sh start
    Tomcat-->>Launcher: process (background)
    
    Launcher->>Launcher: waitForStartup()
    
    loop Until ready or timeout
        Launcher->>Tomcat: Socket connect check
        Tomcat-->>Launcher: connection result
    end
    
    Launcher-->>StartMojo: started
    
    StartMojo->>FS: storePid(catalinaBase, process)
    FS-->>StartMojo: tomcat.pid written
    
    StartMojo-->>Maven: complete
    Maven-->>User: BUILD SUCCESS
    
    Note over User,Tomcat: Tomcat runs in background
```

### tomcat:stop Goal

```mermaid
sequenceDiagram
    participant User
    participant Maven
    participant StopMojo
    participant AbstractMojo as AbstractTomcatMojo
    participant FS as File System
    participant ProcessHandle
    participant Tomcat

    User->>Maven: mvn tomcat:stop
    Maven->>StopMojo: execute()
    
    StopMojo->>AbstractMojo: buildServerConfiguration()
    AbstractMojo-->>StopMojo: ServerConfiguration
    
    StopMojo->>FS: Files.exists(tomcat.pid)
    
    alt PID file exists
        FS-->>StopMojo: true
        StopMojo->>FS: Files.readString(pidFile)
        FS-->>StopMojo: pid
        
        StopMojo->>ProcessHandle: ProcessHandle.of(pid)
        ProcessHandle-->>StopMojo: handle
        
        StopMojo->>ProcessHandle: destroy()
        ProcessHandle->>Tomcat: SIGTERM
        
        StopMojo->>ProcessHandle: onExit().get(timeout)
        
        alt Graceful shutdown
            Tomcat-->>ProcessHandle: terminated
        else Timeout
            StopMojo->>ProcessHandle: destroyForcibly()
            ProcessHandle->>Tomcat: SIGKILL
        end
        
        StopMojo->>FS: Files.deleteIfExists(pidFile)
        
    else PID file not found
        FS-->>StopMojo: false
        StopMojo->>StopMojo: stopViaScript()
        StopMojo->>Tomcat: catalina.sh stop
    end
    
    StopMojo-->>Maven: complete
    Maven-->>User: Tomcat stopped
```

### tomcat:deploy Goal

```mermaid
sequenceDiagram
    participant User
    participant Maven
    participant DeployMojo
    participant AbstractMojo as AbstractTomcatMojo
    participant Deployer as ExplodedWarDeployer
    participant FS as File System

    User->>Maven: mvn tomcat:deploy
    Maven->>DeployMojo: execute()
    
    DeployMojo->>AbstractMojo: buildServerConfiguration()
    AbstractMojo-->>DeployMojo: ServerConfiguration
    
    DeployMojo->>AbstractMojo: buildDeployableConfiguration()
    AbstractMojo-->>DeployMojo: DeployableConfiguration
    
    DeployMojo->>Deployer: redeploy(config)
    
    Deployer->>FS: Files.exists(targetPath)
    
    alt Existing deployment
        FS-->>Deployer: true
        Deployer->>FS: deleteDirectory(targetPath)
    end
    
    Deployer->>Deployer: deploy(config)
    Deployer->>FS: Files.createDirectories(deployDir)
    Deployer->>FS: copyDirectory(source, target)
    
    Deployer-->>DeployMojo: deployed
    DeployMojo-->>Maven: complete
    Maven-->>User: Webapp deployed
```

### Tomcat Download Flow

```mermaid
sequenceDiagram
    participant Mojo as AbstractTomcatMojo
    participant Downloader as TomcatDownloader
    participant Version as TomcatVersion
    participant Validator as ChecksumValidator
    participant HTTP as HttpClient
    participant CDN as Apache CDN
    participant Archive as Apache Archive
    participant FS as File System

    Mojo->>Downloader: download(version, cacheDir, log)
    
    Downloader->>Version: fromVersionString(version)
    Version-->>Downloader: TomcatVersion enum
    
    Downloader->>FS: Files.exists(extractedDir)
    
    alt Already cached and valid
        FS-->>Downloader: true
        Downloader->>FS: isValidTomcatDir(dir)
        FS-->>Downloader: true
        Downloader-->>Mojo: cachedPath
    else Not cached
        FS-->>Downloader: false
        Downloader->>FS: Files.createDirectories(versionDir)
        
        Downloader->>Version: getDownloadUrl(version)
        Version-->>Downloader: primaryUrl
        
        Downloader->>HTTP: send(GET primaryUrl)
        HTTP->>CDN: HTTPS GET
        
        alt Primary download succeeds
            CDN-->>HTTP: 200 OK + zip
            HTTP-->>Downloader: downloaded
            
            Downloader->>Version: getChecksumUrl(version)
            Downloader->>Validator: validate(file, checksumUrl)
            Validator->>CDN: fetch .sha512
            CDN-->>Validator: checksum
            Validator->>Validator: calculateChecksum(file)
            Validator-->>Downloader: valid
            
        else Primary download fails
            CDN-->>HTTP: error
            HTTP-->>Downloader: IOException
            
            Downloader->>Version: getArchiveDownloadUrl(version)
            Version-->>Downloader: archiveUrl
            
            Downloader->>HTTP: send(GET archiveUrl)
            HTTP->>Archive: HTTPS GET
            Archive-->>HTTP: 200 OK + zip
            HTTP-->>Downloader: downloaded
            
            Downloader->>Validator: validate(file, archiveChecksumUrl)
            Validator-->>Downloader: valid
        end
        
        Downloader->>Downloader: extract(zipFile, targetDir)
        Downloader->>FS: ZipInputStream extract
        Downloader->>FS: setExecutablePermissions(bin/*.sh)
        
        Downloader-->>Mojo: extractedPath
    end
```

### Hot Deployment Flow

```mermaid
sequenceDiagram
    participant IDE as IDE/Editor
    participant FS as File System
    participant Watcher as HotDeployWatcher
    participant Deployer as ExplodedWarDeployer
    participant Scheduler as ScheduledExecutor
    participant Tomcat

    Note over Watcher: start() called by RunMojo
    
    Watcher->>FS: WatchService.newWatchService()
    Watcher->>FS: registerRecursive(sourcePath)
    Watcher->>Watcher: start watch thread
    Watcher->>Scheduler: scheduleAtFixedRate(checkAndSync)
    
    loop Watch Thread
        Watcher->>FS: watchService.poll(1s)
        
        alt File changed
            IDE->>FS: Save file
            FS-->>Watcher: WatchEvent(MODIFY)
            Watcher->>Watcher: lastChangeTime = now()
            
            alt New directory created
                FS-->>Watcher: WatchEvent(CREATE, dir)
                Watcher->>FS: registerRecursive(newDir)
            end
        end
    end
    
    loop Scheduler (every N seconds)
        Scheduler->>Watcher: checkAndSync()
        Watcher->>Watcher: elapsed = now - lastChangeTime
        
        alt elapsed >= inactivityLimit
            Watcher->>Watcher: performSync()
            Watcher->>Deployer: redeploy(config)
            Deployer->>FS: deleteDirectory(target)
            Deployer->>FS: copyDirectory(source, target)
            Deployer-->>Watcher: complete
            
            Note over Tomcat: Tomcat detects changes<br/>and reloads context
        end
    end
```

---

## Configuration Flow

```mermaid
flowchart TD
    subgraph "Maven POM"
        POM[pom.xml<br/>plugin configuration]
    end

    subgraph "Command Line"
        CLI[-D properties]
    end

    subgraph "AbstractTomcatMojo"
        Params[Mojo @Parameter fields]
        Build1[buildServerConfiguration]
        Build2[buildDeployableConfiguration]
    end

    subgraph "Configuration Objects"
        SC[ServerConfiguration<br/>Immutable]
        DC[DeployableConfiguration<br/>Immutable]
    end

    subgraph "Services"
        Launcher[TomcatLauncher]
        Deployer[ExplodedWarDeployer]
        Watcher[HotDeployWatcher]
    end

    POM --> Params
    CLI --> Params
    Params --> Build1
    Params --> Build2
    Build1 --> SC
    Build2 --> DC
    SC --> Launcher
    SC --> Deployer
    DC --> Deployer
    DC --> Watcher
```

---

## Design Patterns

### Builder Pattern
Used in `ServerConfiguration` and `DeployableConfiguration` for constructing immutable configuration objects with many optional parameters.

```java
ServerConfiguration config = ServerConfiguration.builder()
    .catalinaHome(path)
    .httpPort(8080)
    .vmOptions(List.of("-Xmx1g"))
    .build();
```

### Template Method Pattern
`AbstractTomcatMojo` defines the skeleton of configuration building (resolving CATALINA_HOME, validating Java version, etc.), while concrete Mojos (`RunMojo`, `StartMojo`, etc.) implement specific execution logic.

### Strategy Pattern
`TomcatVersion` enum encapsulates version-specific behavior (download URLs, minimum Java requirements), allowing the downloader to work with different Tomcat versions without conditional logic.

### Observer Pattern
`HotDeployWatcher` uses Java's `WatchService` to observe file system events and trigger redeployment when files change.

### Facade Pattern
Each Mojo acts as a facade, orchestrating multiple services (downloader, deployer, launcher) to provide a simple interface for Maven users.

---

## Thread Model

```mermaid
flowchart TD
    subgraph "Main Thread"
        Maven[Maven Execution]
        Mojo[Mojo.execute]
    end

    subgraph "Tomcat Process (External)"
        TomcatMain[Tomcat Main Thread]
        TomcatWorkers[HTTP Worker Threads]
    end

    subgraph "Plugin Daemon Threads"
        WatchThread[hot-deploy-watcher<br/>daemon=true]
        SyncThread[hot-deploy-sync<br/>ScheduledExecutor<br/>daemon=true]
    end

    Maven --> Mojo
    Mojo -->|ProcessBuilder.start| TomcatMain
    TomcatMain --> TomcatWorkers
    
    Mojo -->|if autopublish| WatchThread
    Mojo -->|if autopublish| SyncThread
    
    WatchThread -.->|file events| SyncThread
    SyncThread -.->|redeploy| TomcatMain
```

---

## Error Handling Strategy

| Error Type | Handling |
|------------|----------|
| Missing CATALINA_HOME | Auto-download Tomcat |
| Port in use | Fail fast with actionable message |
| Java version mismatch | Fail fast with version requirements |
| Download failure | Fallback to Apache Archive |
| Checksum mismatch | Retry from archive |
| Deployment failure | MojoExecutionException with details |
| Startup timeout | IOException with timeout info |
| Shutdown timeout | Force-kill process |

---

## Security Considerations

1. **Zip Slip Protection**: `TomcatDownloader.extract()` validates paths prevent directory traversal attacks
2. **Checksum Validation**: Downloads are verified using SHA-512 checksums
3. **Shutdown Port Disabled**: `CatalinaBaseGenerator` sets shutdown port to -1
4. **AJP Connector Disabled**: Commented out for development use
5. **No Credential Storage**: Plugin does not store any credentials

---

## Related Documentation

- [README.md](../README.md) - Usage and configuration
- [CONTRIBUTING.md](../CONTRIBUTING.md) - Development guidelines
- [PUBLISHING.md](PUBLISHING.md) - Release process
