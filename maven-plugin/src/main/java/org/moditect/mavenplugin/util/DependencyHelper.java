/*
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Copyright The original authors
 *
 *  Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.moditect.mavenplugin.util;

import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.project.MavenProject;
import org.moditect.model.DependencyJar;

public class DependencyHelper {

    public static Set<DependencyJar> getDirectAndTransitiveDependencies(MavenProject project) {
        return project.getArtifacts()
                .stream()
                .map(a -> new DependencyJar(
                        a.getGroupId(),
                        a.getArtifactId(),
                        a.getVersion(),
                        a.getFile().toPath()))
                .collect(Collectors.toSet());
    }

}
