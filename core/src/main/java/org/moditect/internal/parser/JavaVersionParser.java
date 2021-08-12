/**
 *  Copyright 2017 - 2018 The ModiTect authors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.moditect.internal.parser;

import org.moditect.spi.log.Log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper to extract and parse the Java version.
 * Alternatively from Java 9 it is possible to use the API java.lang.Runtime#version().
 *
 * @author Fabio Massimo Ercoli
 */
public final class JavaVersionParser {

    private static final String VERSION_REGEXP = "^(\\d+)\\.(\\d+)\\.(\\d+).*";
    private static final Pattern VERSION_PATTERN = Pattern.compile(VERSION_REGEXP);
    private static final String JAVA_VERSION_PROPERTY_NAME = "java.version";

    private final Log log;

    public JavaVersionParser(Log log) {
        this.log = log;
    }

    JavaVersionParser() {
        this.log = null;
    }

    public Version javaVersion() {
        return javaVersion(System.getProperty(JAVA_VERSION_PROPERTY_NAME));
    }

    Version javaVersion(String versionString) {
        Matcher matcher = VERSION_PATTERN.matcher(versionString);
        if (!matcher.matches()) {
            if (log != null) {
                log.warn("The java version " + versionString + " cannot be parsed as " + VERSION_REGEXP);
            }

            return null;
        }

        try {
            return new Version(Integer.parseInt(matcher.group(1)),
                    Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3)));
        } catch (IndexOutOfBoundsException | NumberFormatException ex) {
            if (log != null) {
                log.error("The java version " + versionString + " has an invalid format. " + ex.getMessage());
            }

            return null;
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

        public int major() {
            return major;
        }
        public int minor() {
            return minor;
        }
        public int mini() {
            return mini;
        }
    }
}
