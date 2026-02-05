package io.github.rajendarreddyj.tomcat.config;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable configuration for Tomcat server settings.
 * Use the {@link Builder} to construct instances.
 */
public final class ServerConfiguration {

    private final Path catalinaHome;
    private final Path catalinaBase;
    private final String httpHost;
    private final int httpPort;
    private final Path javaHome;
    private final List<String> vmOptions;
    private final Map<String, String> environmentVariables;
    private final long startupTimeout;
    private final long shutdownTimeout;
    private final List<String> classpathAdditions;

    private ServerConfiguration(Builder builder) {
        this.catalinaHome = Objects.requireNonNull(builder.catalinaHome, "catalinaHome is required");
        this.catalinaBase = builder.catalinaBase != null ? builder.catalinaBase : builder.catalinaHome;
        this.httpHost = builder.httpHost != null ? builder.httpHost : "localhost";
        this.httpPort = builder.httpPort > 0 ? builder.httpPort : 8080;
        this.javaHome = builder.javaHome;
        this.vmOptions = builder.vmOptions != null
                ? List.copyOf(builder.vmOptions)
                : List.of();
        this.environmentVariables = builder.environmentVariables != null
                ? Map.copyOf(builder.environmentVariables)
                : Map.of();
        this.startupTimeout = builder.startupTimeout > 0 ? builder.startupTimeout : 120000L;
        this.shutdownTimeout = builder.shutdownTimeout > 0 ? builder.shutdownTimeout : 30000L;
        this.classpathAdditions = builder.classpathAdditions != null
                ? List.copyOf(builder.classpathAdditions)
                : List.of();
    }

    /**
     * Gets the Tomcat installation directory (CATALINA_HOME).
     *
     * @return the CATALINA_HOME path
     */
    public Path getCatalinaHome() {
        return catalinaHome;
    }

    /**
     * Gets the Tomcat instance directory (CATALINA_BASE).
     *
     * @return the CATALINA_BASE path
     */
    public Path getCatalinaBase() {
        return catalinaBase;
    }

    /**
     * Gets the HTTP host to bind to.
     *
     * @return the HTTP host
     */
    public String getHttpHost() {
        return httpHost;
    }

    /**
     * Gets the HTTP port.
     *
     * @return the HTTP port
     */
    public int getHttpPort() {
        return httpPort;
    }

    /**
     * Gets the Java home directory.
     *
     * @return the Java home path, or null to use system default
     */
    public Path getJavaHome() {
        return javaHome;
    }

    /**
     * Gets the JVM options.
     *
     * @return an unmodifiable list of JVM options
     */
    public List<String> getVmOptions() {
        return vmOptions;
    }

    /**
     * Gets the environment variables.
     *
     * @return an unmodifiable map of environment variables
     */
    public Map<String, String> getEnvironmentVariables() {
        return environmentVariables;
    }

    /**
     * Gets the startup timeout in milliseconds.
     *
     * @return the startup timeout
     */
    public long getStartupTimeout() {
        return startupTimeout;
    }

    /**
     * Gets the shutdown timeout in milliseconds.
     *
     * @return the shutdown timeout
     */
    public long getShutdownTimeout() {
        return shutdownTimeout;
    }

    /**
     * Gets the additional classpath entries.
     *
     * @return an unmodifiable list of classpath additions
     */
    public List<String> getClasspathAdditions() {
        return classpathAdditions;
    }

    /**
     * Creates a new Builder instance.
     *
     * @return a new Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for ServerConfiguration.
     */
    public static final class Builder {
        private Path catalinaHome;
        private Path catalinaBase;
        private String httpHost;
        private int httpPort;
        private Path javaHome;
        private List<String> vmOptions;
        private Map<String, String> environmentVariables;
        private long startupTimeout;
        private long shutdownTimeout;
        private List<String> classpathAdditions;

        private Builder() {
        }

        /**
         * Sets the CATALINA_HOME directory.
         *
         * @param catalinaHome the Tomcat installation directory
         * @return this builder
         */
        public Builder catalinaHome(Path catalinaHome) {
            this.catalinaHome = catalinaHome;
            return this;
        }

        /**
         * Sets the CATALINA_BASE directory.
         *
         * @param catalinaBase the Tomcat instance directory
         * @return this builder
         */
        public Builder catalinaBase(Path catalinaBase) {
            this.catalinaBase = catalinaBase;
            return this;
        }

        /**
         * Sets the HTTP host.
         *
         * @param httpHost the host to bind to
         * @return this builder
         */
        public Builder httpHost(String httpHost) {
            this.httpHost = httpHost;
            return this;
        }

        /**
         * Sets the HTTP port.
         *
         * @param httpPort the port number
         * @return this builder
         */
        public Builder httpPort(int httpPort) {
            this.httpPort = httpPort;
            return this;
        }

        /**
         * Sets the Java home directory.
         *
         * @param javaHome the Java installation directory
         * @return this builder
         */
        public Builder javaHome(Path javaHome) {
            this.javaHome = javaHome;
            return this;
        }

        /**
         * Sets the JVM options.
         *
         * @param vmOptions the JVM options
         * @return this builder
         */
        public Builder vmOptions(List<String> vmOptions) {
            this.vmOptions = vmOptions;
            return this;
        }

        /**
         * Sets the environment variables.
         *
         * @param environmentVariables the environment variables
         * @return this builder
         */
        public Builder environmentVariables(Map<String, String> environmentVariables) {
            this.environmentVariables = environmentVariables != null
                    ? new HashMap<>(environmentVariables)
                    : null;
            return this;
        }

        /**
         * Sets the startup timeout.
         *
         * @param startupTimeout the timeout in milliseconds
         * @return this builder
         */
        public Builder startupTimeout(long startupTimeout) {
            this.startupTimeout = startupTimeout;
            return this;
        }

        /**
         * Sets the shutdown timeout.
         *
         * @param shutdownTimeout the timeout in milliseconds
         * @return this builder
         */
        public Builder shutdownTimeout(long shutdownTimeout) {
            this.shutdownTimeout = shutdownTimeout;
            return this;
        }

        /**
         * Sets the additional classpath entries.
         *
         * @param classpathAdditions the classpath additions
         * @return this builder
         */
        public Builder classpathAdditions(List<String> classpathAdditions) {
            this.classpathAdditions = classpathAdditions;
            return this;
        }

        /**
         * Builds the ServerConfiguration.
         *
         * @return the ServerConfiguration instance
         */
        public ServerConfiguration build() {
            return new ServerConfiguration(this);
        }
    }

    @Override
    public String toString() {
        return "ServerConfiguration{" +
                "catalinaHome=" + catalinaHome +
                ", catalinaBase=" + catalinaBase +
                ", httpHost='" + httpHost + '\'' +
                ", httpPort=" + httpPort +
                ", javaHome=" + javaHome +
                ", vmOptions=" + vmOptions +
                ", startupTimeout=" + startupTimeout +
                ", shutdownTimeout=" + shutdownTimeout +
                ", classpathAdditions=" + classpathAdditions +
                '}';
    }
}
