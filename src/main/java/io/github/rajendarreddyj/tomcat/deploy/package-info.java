/**
 * Deployment utilities for the Tomcat Maven Plugin.
 *
 * <p>
 * This package contains classes for deploying web applications to Tomcat
 * and monitoring for file changes to enable hot redeployment.
 * </p>
 *
 * <h2>Deployment Modes</h2>
 * <ul>
 * <li><strong>Exploded WAR Deployment</strong> - Copies files from the build
 * directory
 * to Tomcat's webapps folder, enabling rapid development cycles</li>
 * <li><strong>Hot Deployment</strong> - Automatically redeploys when source
 * files change</li>
 * </ul>
 *
 * <h2>Classes</h2>
 * <ul>
 * <li>{@link io.github.rajendarreddyj.tomcat.deploy.ExplodedWarDeployer} -
 * Handles deployment and redeployment of exploded WAR directories</li>
 * <li>{@link io.github.rajendarreddyj.tomcat.deploy.HotDeployWatcher} -
 * Watches for file changes and triggers automatic redeployment</li>
 * </ul>
 *
 * <h2>Auto-Publish</h2>
 * <p>
 * When auto-publish is enabled, the
 * {@link io.github.rajendarreddyj.tomcat.deploy.HotDeployWatcher}
 * monitors the source directory for changes. After a configurable period of
 * inactivity
 * (default: 30 seconds), changes are automatically deployed to Tomcat.
 * </p>
 *
 * @author rajendarreddyj
 * @see io.github.rajendarreddyj.tomcat.deploy.ExplodedWarDeployer
 * @see io.github.rajendarreddyj.tomcat.deploy.HotDeployWatcher
 * @since 1.0.0
 */
package io.github.rajendarreddyj.tomcat.deploy;
