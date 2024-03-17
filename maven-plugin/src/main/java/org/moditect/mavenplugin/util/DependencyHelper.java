/*
 *  SPDX-License-Identifier: Apache-2.0
 *
 *   Copyright The original authors
 *
 *   Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.moditect.mavenplugin.util;

import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.project.MavenProject;

public class DependencyHelper {

    public static Set<Path> getDirectAndTransitiveDependencies(MavenProject project) {
        return project.getArtifacts()
                .stream()
                .map(a -> a.getFile().toPath())
                .collect(Collectors.toSet());
    }

}
