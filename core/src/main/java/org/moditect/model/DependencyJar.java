/*
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Copyright The original authors
 *
 *  Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.moditect.model;

import java.nio.file.Path;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

public class DependencyJar {

    private final String groupId;
    private final String artifactId;
    private final String version;
    private final Path jarPath;

    public DependencyJar(String groupId, String artifactId, String version, Path jarPath) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.jarPath = jarPath;
    }

    public String groupId() {
        return groupId;
    }

    public String artifactId() {
        return artifactId;
    }

    public String version() {
        return version;
    }

    public Path jarPath() {
        return jarPath;
    }

    /**
     * Copes with non-unique dependency jar filenames by selectively prepending the groupId.
     * @param destDir The directory where we want to deposit the dependency jars
     * @param dependencies The set of dependencies that we want to deposit
     * @return A list of file copy operations
     */
    public static List<FileCopy> uniqueDestinations(Path destDir, Set<DependencyJar> dependencies) {
        Set<Path> nonUniqueJarNames = dependencies.stream()
                .map(dj -> dj.jarPath().getFileName())
                .collect(Collectors.toMap(
                        k -> k,
                        v -> 1,
                        (a, b) -> a + b))
                .entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .map(Entry::getKey)
                .collect(toSet());

        return dependencies.stream()
                .map(d -> {
                    Path sink;
                    if (nonUniqueJarNames.contains(d.jarPath().getFileName())) {
                        sink = destDir.resolve(
                                d.groupId() + "." + d.jarPath().getFileName().toString());
                    }
                    else {
                        sink = destDir.resolve(d.jarPath().getFileName());
                    }
                    return new FileCopy(d.jarPath(), sink);
                })
                .collect(toList());
    }

    public static class FileCopy {
        private final Path source;
        private final Path sink;

        public FileCopy(Path source, Path sink) {
            this.source = source;
            this.sink = sink;
        }

        public Path source() {
            return source;
        }

        public Path sink() {
            return sink;
        }
    }
}
