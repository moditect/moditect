/*
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Copyright The original authors
 *
 *  Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.moditect.model;

import java.nio.file.Path;

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
}
