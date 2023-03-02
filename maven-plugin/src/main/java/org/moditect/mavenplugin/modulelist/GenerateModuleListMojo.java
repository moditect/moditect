/*
 *  Copyright 2017 - 2023 The ModiTect authors
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
package org.moditect.mavenplugin.modulelist;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.moditect.commands.GenerateModuleList;
import org.moditect.mavenplugin.util.DependencyHelper;
import org.moditect.mavenplugin.util.MojoLog;
import org.moditect.model.Version;

import java.io.File;

@Mojo(name = "list-application-image-modules",
		defaultPhase = LifecyclePhase.PACKAGE,
		requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME
)
public class GenerateModuleListMojo extends AbstractMojo {

	@Parameter(defaultValue = "${project}", readonly = true)
	protected MavenProject project;

	@Parameter(property = "moditect.jvmVersion")
	private Integer jvmVersion;

	@Override
	public void execute() throws MojoExecutionException {
		GenerateModuleList generateModuleList = new GenerateModuleList(
				new File(project.getBuild().getOutputDirectory()).toPath(),
				// project.getArtifact().getFile().toPath(),
				DependencyHelper.getDirectAndTransitiveDependencies(project),
				determineJvmVersion(),
				new MojoLog(getLog()));
		try {
			generateModuleList.run();
		} catch (RuntimeException ex) {
			getLog().error(ex);
			throw new MojoExecutionException("Error generating module list", ex);
		}
	}

	private Version determineJvmVersion() {
		if (jvmVersion != null) {
			return Version.valueOf(jvmVersion);
		}

		Object rawVersion = project.getProperties().get("maven.compiler.release");
		if (rawVersion != null) {
			return Version.valueOf(rawVersion);
		}

		rawVersion = project.getProperties().get("maven.compiler.target");
		if (rawVersion != null) {
			return Version.valueOf(rawVersion);
		}

		throw new IllegalStateException("Couldn't determine target version, please specify the 'targetVersion' configuration property");
	}
}
