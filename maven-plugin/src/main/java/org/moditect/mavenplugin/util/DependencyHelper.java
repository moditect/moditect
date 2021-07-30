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
package org.moditect.mavenplugin.util;

import org.apache.maven.project.MavenProject;

import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

public class DependencyHelper {

	public static Set<Path> getDirectAndTransitiveDependencies(MavenProject project) {
		return project.getArtifacts()
				.stream()
				.map(a -> a.getFile().toPath())
				.collect(Collectors.toSet());
	}

}
