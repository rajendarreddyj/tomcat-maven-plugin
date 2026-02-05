package io.github.rajendarreddyj.tomcat.deploy;

import io.github.rajendarreddyj.tomcat.config.DeployableConfiguration;
import org.apache.maven.plugin.logging.Log;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Deploys exploded WAR directories to Tomcat webapps.
 */
public class ExplodedWarDeployer {

    private final Log log;

    /**
     * Creates a new ExplodedWarDeployer with the given logger.
     *
     * @param log the Maven logger
     */
    public ExplodedWarDeployer(Log log) {
        this.log = log;
    }

    /**
     * Deploys the webapp to the configured location.
     *
     * @param config Deployment configuration
     * @throws IOException if deployment fails
     */
    public void deploy(DeployableConfiguration config) throws IOException {
        Path sourcePath = config.getSourcePath();
        Path deployDir = config.getDeployDir();
        String targetName = config.getTargetDirectoryName();
        Path targetPath = deployDir.resolve(targetName);

        if (!Files.exists(sourcePath)) {
            throw new IOException("Source path does not exist: " + sourcePath);
        }

        log.info("Deploying " + config.getModuleName() + " to " + targetPath);
        log.debug("Source: " + sourcePath);
        log.debug("Context path: " + config.getContextPath());

        // Clean existing deployment
        if (Files.exists(targetPath)) {
            log.info("Removing existing deployment: " + targetPath);
            deleteDirectory(targetPath);
        }

        // Create webapps directory if needed
        Files.createDirectories(deployDir);

        // Copy webapp
        copyDirectory(sourcePath, targetPath);

        log.info("Deployment complete: " + targetName);
    }

    /**
     * Redeploys the webapp (remove and recreate).
     *
     * @param config Deployment configuration
     * @throws IOException if redeployment fails
     */
    public void redeploy(DeployableConfiguration config) throws IOException {
        Path targetPath = config.getDeployDir().resolve(config.getTargetDirectoryName());

        if (Files.exists(targetPath)) {
            log.info("Undeploying existing application...");
            deleteDirectory(targetPath);
        }

        deploy(config);
    }

    /**
     * Synchronizes changed files from source to deployed webapp.
     * Used for hot deployment.
     *
     * @param config      Deployment configuration
     * @param changedFile the file that changed
     * @throws IOException if sync fails
     */
    public void syncChanges(DeployableConfiguration config, Path changedFile) throws IOException {
        Path sourcePath = config.getSourcePath();
        Path targetDir = config.getDeployDir().resolve(config.getTargetDirectoryName());

        // Calculate relative path
        Path relativePath = sourcePath.relativize(changedFile);
        Path targetFile = targetDir.resolve(relativePath);

        log.debug("Syncing changed file: " + relativePath);

        Files.createDirectories(targetFile.getParent());
        Files.copy(changedFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Copies a directory tree recursively.
     */
    private void copyDirectory(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                    throws IOException {
                Path targetDir = target.resolve(source.relativize(dir));
                Files.createDirectories(targetDir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                Path targetFile = target.resolve(source.relativize(file));
                Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Deletes a directory and all its contents.
     */
    private void deleteDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            try (Stream<Path> walk = Files.walk(directory)) {
                walk.sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                log.warn("Could not delete: " + path);
                            }
                        });
            }
        }
    }
}
