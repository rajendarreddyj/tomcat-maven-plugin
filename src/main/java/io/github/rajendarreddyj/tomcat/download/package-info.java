/**
 * Download and extraction utilities for the Tomcat Maven Plugin.
 *
 * <p>
 * This package contains classes responsible for downloading Apache Tomcat
 * distributions from official mirrors and validating their integrity.
 * </p>
 *
 * <h2>Download Process</h2>
 * <ol>
 * <li>Check if Tomcat is already cached locally</li>
 * <li>Download from primary Apache mirror (dlcdn.apache.org)</li>
 * <li>Fallback to Apache archive if primary fails</li>
 * <li>Validate SHA-512 checksum</li>
 * <li>Extract to cache directory</li>
 * </ol>
 *
 * <h2>Classes</h2>
 * <ul>
 * <li>{@link io.github.rajendarreddyj.tomcat.download.TomcatDownloader} -
 * Downloads and extracts Tomcat distributions with caching support</li>
 * <li>{@link io.github.rajendarreddyj.tomcat.download.ChecksumValidator} -
 * Validates file integrity using SHA-512 checksums</li>
 * </ul>
 *
 * <h2>Caching</h2>
 * <p>
 * Downloaded distributions are cached in {@code ~/.m2/tomcat-cache} by default.
 * The cache directory can be configured via the {@code tomcat.cache.dir}
 * property.
 * </p>
 *
 * @author rajendarreddyj
 * @see io.github.rajendarreddyj.tomcat.download.TomcatDownloader
 * @see io.github.rajendarreddyj.tomcat.download.ChecksumValidator
 * @since 1.0.0
 */
package io.github.rajendarreddyj.tomcat.download;
