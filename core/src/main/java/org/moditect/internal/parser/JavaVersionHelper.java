/*
 *  SPDX-License-Identifier: Apache-2.0
 *
 *   Copyright The original authors
 *
 *   Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.moditect.internal.parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.moditect.spi.log.Log;

/**
 * Helper to extract and parse the current Java version to check if multi release path should be used or not.
 *
 * @author Fabio Massimo Ercoli
 */
public final class JavaVersionHelper {

    private static final String VERSION_REGEXP = "^(\\d+)\\.(\\d+)\\.(\\d+).*";
    private static final Pattern VERSION_PATTERN = Pattern.compile(VERSION_REGEXP);
    private static final String JAVA_VERSION_PROPERTY_NAME = "java.version";

    private final Log log;

    JavaVersionHelper() {
        this.log = null;
    }

    public JavaVersionHelper(Log log) {
        this.log = log;
    }

    public boolean resolveWithVersionIfMultiRelease() {
        Version version = javaVersion();
        if (version == null) {
            return false;
        }

        if (version.major >= 14) {
            debug("Detected JDK 14+");
            return true;
        }

        // See https://github.com/moditect/moditect/issues/141
        if (version.major == 11 && version.minor == 0 && version.mini >= 11) {
            debug("Detected JDK 11.0.11+");
            return true;
        }
        return false;
    }

    Version javaVersion() {
        String versionString = System.getProperty(JAVA_VERSION_PROPERTY_NAME);
        debug(JAVA_VERSION_PROPERTY_NAME + " -> " + versionString);

        return javaVersion(versionString);
    }

    Version javaVersion(String versionString) {
        Matcher matcher = VERSION_PATTERN.matcher(versionString);
        if (!matcher.matches()) {
            warn("The java version " + versionString + " cannot be parsed as " + VERSION_REGEXP);
            return null;
        }

        try {
            Version version = new Version(Integer.parseInt(matcher.group(1)),
                    Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3)));

            debug("parsed.version -> " + version);
            return version;
        }
        catch (IndexOutOfBoundsException | NumberFormatException ex) {
            error("The java version " + versionString + " has an invalid format. " + ex.getMessage());
            return null;
        }
    }

    private void debug(String message) {
        if (log != null) {
            log.debug(message);
        }
    }

    private void warn(String message) {
        if (log != null) {
            log.warn(message);
        }
    }

    private void error(String message) {
        if (log != null) {
            log.error(message);
        }
    }

    static class Version {
        private int major;
        private int minor;
        private int mini;

        private Version(int major, int minor, int mini) {
            this.major = major;
            this.minor = minor;
            this.mini = mini;
        }

        int major() {
            return major;
        }

        int minor() {
            return minor;
        }

        int mini() {
            return mini;
        }

        @Override
        public String toString() {
            return major + "." + minor + "." + mini;
        }
    }
}
