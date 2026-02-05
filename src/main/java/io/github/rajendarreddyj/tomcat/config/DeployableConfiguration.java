package io.github.rajendarreddyj.tomcat.config;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Immutable configuration for deployment settings.
 * Use the {@link Builder} to construct instances.
 *
 * @author rajendarreddyj
 * @since 1.0.0
 */
public final class DeployableConfiguration {

    /** The module name (typically the Maven artifact ID). */
    private final String moduleName;

    /** The source path of the web application to deploy. */
    private final Path sourcePath;

    /** The context path for the deployed application. */
    private final String contextPath;

    /** The deployment directory (webapps folder). */
    private final Path deployDir;

    /** Whether auto-publish is enabled for hot deployment. */
    private final boolean autopublishEnabled;

    /** Seconds of inactivity before auto-publish triggers. */
    private final int autopublishInactivityLimit;

    /** The output directory name for deployment. */
    private final String deploymentOutputName;

    /**
     * Constructs a DeployableConfiguration from builder values.
     *
     * @param builder the builder containing configuration values
     */
    private DeployableConfiguration(Builder builder) {
        this.moduleName = Objects.requireNonNull(builder.moduleName, "moduleName is required");
        this.sourcePath = Objects.requireNonNull(builder.sourcePath, "sourcePath is required");
        this.contextPath = normalizeContextPath(builder.contextPath);
        this.deployDir = builder.deployDir;
        this.autopublishEnabled = builder.autopublishEnabled;
        this.autopublishInactivityLimit = builder.autopublishInactivityLimit > 0
                ? builder.autopublishInactivityLimit
                : 30;
        this.deploymentOutputName = builder.deploymentOutputName;
    }

    /**
     * Normalizes the context path to ensure it starts with "/" and handles ROOT.
     *
     * @param contextPath the context path to normalize
     * @return the normalized context path
     */
    private static String normalizeContextPath(String contextPath) {
        if (contextPath == null || contextPath.isBlank() || "/".equals(contextPath)) {
            return "/";
        }
        String normalized = contextPath.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        // Remove trailing slash if present (except for root)
        if (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    /**
     * Gets the module name (typically the artifact ID).
     *
     * @return the module name
     */
    public String getModuleName() {
        return moduleName;
    }

    /**
     * Gets the source path of the web application.
     *
     * @return the source path
     */
    public Path getSourcePath() {
        return sourcePath;
    }

    /**
     * Gets the context path.
     *
     * @return the normalized context path
     */
    public String getContextPath() {
        return contextPath;
    }

    /**
     * Gets the deployment directory.
     *
     * @return the deployment directory, or null to use default
     */
    public Path getDeployDir() {
        return deployDir;
    }

    /**
     * Checks if auto-publish is enabled.
     *
     * @return true if auto-publish is enabled
     */
    public boolean isAutopublishEnabled() {
        return autopublishEnabled;
    }

    /**
     * Gets the auto-publish inactivity limit in seconds.
     *
     * @return the inactivity limit
     */
    public int getAutopublishInactivityLimit() {
        return autopublishInactivityLimit;
    }

    /**
     * Gets the deployment output name.
     *
     * @return the deployment output name, or null to derive from context path
     */
    public String getDeploymentOutputName() {
        return deploymentOutputName;
    }

    /**
     * Derives the target directory name for deployment.
     * If deploymentOutputName is set, uses that. Otherwise, derives from context
     * path.
     * ROOT context ("/") maps to "ROOT", other contexts map to their path without
     * leading slash.
     *
     * @return the target directory name
     */
    public String getTargetDirectoryName() {
        if (deploymentOutputName != null && !deploymentOutputName.isBlank()) {
            return deploymentOutputName;
        }
        if ("/".equals(contextPath)) {
            return "ROOT";
        }
        // Remove leading slash and replace remaining slashes with #
        return contextPath.substring(1).replace('/', '#');
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
     * Builder for DeployableConfiguration.
     */
    public static final class Builder {

        /** The module name (typically the Maven artifact ID). */
        private String moduleName;

        /** The source path of the web application to deploy. */
        private Path sourcePath;

        /** The context path for the deployed application. */
        private String contextPath;

        /** The deployment directory (webapps folder). */
        private Path deployDir;

        /** Whether auto-publish is enabled for hot deployment. */
        private boolean autopublishEnabled;

        /** Seconds of inactivity before auto-publish triggers. */
        private int autopublishInactivityLimit;

        /** The output directory name for deployment. */
        private String deploymentOutputName;

        /**
         * Private constructor for Builder.
         */
        private Builder() {
        }

        /**
         * Sets the module name.
         *
         * @param moduleName the module name
         * @return this builder
         */
        public Builder moduleName(String moduleName) {
            this.moduleName = moduleName;
            return this;
        }

        /**
         * Sets the source path.
         *
         * @param sourcePath the source path
         * @return this builder
         */
        public Builder sourcePath(Path sourcePath) {
            this.sourcePath = sourcePath;
            return this;
        }

        /**
         * Sets the context path.
         *
         * @param contextPath the context path
         * @return this builder
         */
        public Builder contextPath(String contextPath) {
            this.contextPath = contextPath;
            return this;
        }

        /**
         * Sets the deployment directory.
         *
         * @param deployDir the deployment directory
         * @return this builder
         */
        public Builder deployDir(Path deployDir) {
            this.deployDir = deployDir;
            return this;
        }

        /**
         * Sets whether auto-publish is enabled.
         *
         * @param autopublishEnabled true to enable auto-publish
         * @return this builder
         */
        public Builder autopublishEnabled(boolean autopublishEnabled) {
            this.autopublishEnabled = autopublishEnabled;
            return this;
        }

        /**
         * Sets the auto-publish inactivity limit.
         *
         * @param autopublishInactivityLimit the limit in seconds
         * @return this builder
         */
        public Builder autopublishInactivityLimit(int autopublishInactivityLimit) {
            this.autopublishInactivityLimit = autopublishInactivityLimit;
            return this;
        }

        /**
         * Sets the deployment output name.
         *
         * @param deploymentOutputName the output name
         * @return this builder
         */
        public Builder deploymentOutputName(String deploymentOutputName) {
            this.deploymentOutputName = deploymentOutputName;
            return this;
        }

        /**
         * Builds the DeployableConfiguration.
         *
         * @return the DeployableConfiguration instance
         */
        public DeployableConfiguration build() {
            return new DeployableConfiguration(this);
        }
    }

    @Override
    public String toString() {
        return "DeployableConfiguration{" +
                "moduleName='" + moduleName + '\'' +
                ", sourcePath=" + sourcePath +
                ", contextPath='" + contextPath + '\'' +
                ", deployDir=" + deployDir +
                ", autopublishEnabled=" + autopublishEnabled +
                ", autopublishInactivityLimit=" + autopublishInactivityLimit +
                ", deploymentOutputName='" + deploymentOutputName + '\'' +
                '}';
    }
}
