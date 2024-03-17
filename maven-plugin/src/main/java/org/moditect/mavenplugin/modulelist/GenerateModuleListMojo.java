/*
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Copyright The original authors
 *
 *  Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.moditect.mavenplugin.modulelist;

import java.io.File;

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

@Mojo(name = "list-application-image-modules", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, threadSafe = true)
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
        }
        catch (RuntimeException ex) {
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
