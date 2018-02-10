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
package org.moditect.mavenplugin.mojo.generate;

import java.io.File;
import java.util.*;

import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.moditect.generator.ModuleInfoGenerator;
import org.moditect.mavenplugin.internal.MavenArtifactResolver;
import org.moditect.mavenplugin.log.MojoLog;
import org.moditect.model.generate.ModuleConfiguration;
import org.moditect.generator.ArtifactResolutionHelper;

/**
 * @author Gunnar Morling
 */
@Mojo(name = "generate-module-info", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class GenerateModuleInfoMojo extends AbstractMojo {

    @Component
    private ArtifactHandlerManager artifactHandlerManager;

    @Parameter( defaultValue = "${session}", readonly = true, required = true )
    private MavenSession session;

    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    @Component
    private RepositorySystem repoSystem;

    @Parameter( defaultValue = "${repositorySystemSession}", readonly = true, required = true )
    private RepositorySystemSession repoSession;

    @Parameter( defaultValue = "${project.remoteProjectRepositories}", readonly = true, required = true )
    private List<RemoteRepository> remoteRepos;

    @Parameter(readonly = true, defaultValue = "${project.build.directory}/moditect")
    private File workingDirectory;

    @Parameter(property = "outputDirectory", defaultValue = "${project.build.directory}/generated-sources/modules")
    private File outputDirectory;

    @Parameter
    private List<ModuleConfiguration> modules;

    @Parameter
    private List<String> jdepsExtraArgs;

    @Parameter(property = "moditect.artifact")
    private String artifactOverride;

    @Parameter(property = "moditect.additionalDependencies")
    private String additionalDependenciesOverride;

    @Parameter(property = "moditect.moduleName")
    private String moduleNameOverride;

    @Parameter(property = "moditect.exportExcludes")
    private String exportExcludesOverride;

    @Parameter(property = "moditect.addServiceUses", defaultValue = "false")
    private boolean addServiceUsesOverride;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final ArtifactResolutionHelper artifactResolutionHelper =
                new ArtifactResolutionHelper( repoSystem, repoSession, remoteRepos );
        final ModuleInfoGenerator moduleInfoGenerator = new ModuleInfoGenerator(new MavenArtifactResolver(project)
                , artifactResolutionHelper, jdepsExtraArgs, new MojoLog(getLog())
                , workingDirectory, outputDirectory, modules,artifactOverride,moduleNameOverride,
                additionalDependenciesOverride,exportExcludesOverride,addServiceUsesOverride);
        try {
            moduleInfoGenerator.generate();
        } catch (Exception e) {
            throw new MojoExecutionException("Problem generating module configuration",e);
        }
    }

}
