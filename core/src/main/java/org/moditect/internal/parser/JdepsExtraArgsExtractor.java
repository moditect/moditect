/*
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

import java.util.List;
import java.util.Optional;
import java.util.jar.JarFile;

public final class JdepsExtraArgsExtractor {

    public static final String MULTI_RELEASE_ARGUMENT = "--multi-release";

    private final Log log;

    public JdepsExtraArgsExtractor(Log log) {
        this.log = log;
    }

    public Optional<Integer> extractVersion(List<String> jdepsExtraArgs) {
        for (int i = 0; i < jdepsExtraArgs.size(); i++) {
            String extraArg = jdepsExtraArgs.get(i);

            if (extraArg.startsWith(MULTI_RELEASE_ARGUMENT)) {
                if (extraArg.length() == MULTI_RELEASE_ARGUMENT.length()) {
                    // we expect the version number in the next argument
                    return extractVersionFromNextArgument(jdepsExtraArgs, i);
                }
                return extractVersionFromSameArgument(extraArg);
            }
        }

        debug("No version can be extracted from arguments: " + jdepsExtraArgs);
        return Optional.empty();
    }

    private Optional<Integer> extractVersionFromNextArgument(List<String> jdepsExtraArgs, int i) {
        if (i == jdepsExtraArgs.size() - 1) {
            // there is no next argument
            error("No argument value for " + MULTI_RELEASE_ARGUMENT);
            return Optional.empty();
        }

        String versionString = jdepsExtraArgs.get(i + 1);
        debug("Version extracted from the next argument: " + versionString);
        return parseVersionNumber(versionString);
    }

    private Optional<Integer> extractVersionFromSameArgument(String multiReleaseArgument) {
        if (multiReleaseArgument.length() < MULTI_RELEASE_ARGUMENT.length() + 2) {
            error("Invalid argument value for " + MULTI_RELEASE_ARGUMENT + ": " + multiReleaseArgument);
            return Optional.empty();
        }

        String versionString = multiReleaseArgument.substring(MULTI_RELEASE_ARGUMENT.length()+1);
        debug("Version extracted from the same argument: " + versionString);
        return parseVersionNumber(versionString);
    }

    private Optional<Integer> parseVersionNumber(String versionString) {
        if ("base".equals(versionString)) {
            // "base" basically means "put the file at the root instead of inside versions/<some-version>"
            // See https://github.com/openjdk/jdk/blob/5740a3b6e635456b34b4f31d0f1e84d3e746b796/src/jdk.jdeps/share/classes/com/sun/tools/jdeps/JdepsTask.java#L274-L275
            // See https://github.com/openjdk/jdk/blob/5740a3b6e635456b34b4f31d0f1e84d3e746b796/src/java.base/share/classes/java/util/jar/JarFile.java#L179
            // See https://github.com/openjdk/jdk/blob/5740a3b6e635456b34b4f31d0f1e84d3e746b796/src/java.base/share/classes/java/util/jar/JarFile.java#L604
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.parseInt(versionString));
        } catch (NumberFormatException ex) {
            error("Invalid argument value for " + MULTI_RELEASE_ARGUMENT + ": " + versionString);
            return Optional.empty();
        }
    }

    private void debug(String message) {
        if (log != null) {
            log.debug(message);
        }
    }

    private void error(String message) {
        if (log != null) {
            log.error(message);
        }
    }
}
