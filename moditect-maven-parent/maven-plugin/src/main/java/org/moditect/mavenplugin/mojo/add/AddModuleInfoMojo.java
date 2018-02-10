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
package org.moditect.mavenplugin.mojo.add;

import java.io.File;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.moditect.mavenplugin.internal.MavenArtifactResolver;
import org.moditect.mavenplugin.log.MojoLog;
import org.moditect.model.add.AddModuleConfiguration;
import org.moditect.model.add.MainModuleConfiguration;
import org.moditect.generator.ModuleInfoGenerator;
import org.moditect.generator.ArtifactResolutionHelper;

/**
 * @author Gunnar Morling
 */
@Mojo(name = "add-module-info", defaultPhase = LifecyclePhase.PROCESS_RESOURCES, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class AddModuleInfoMojo extends AbstractMojo {

    @Component
    private RepositorySystem repoSystem;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true, required = true)
    private RepositorySystemSession repoSession;

    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true, required = true)
    private List<RemoteRepository> remoteRepos;

    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    @Parameter(defaultValue = "${project.artifactId}", readonly = true, required = true)
    private String artifactId;

    @Parameter(defaultValue = "${project.version}", readonly = true, required = true)
    private String version;

    @Parameter(readonly = true, defaultValue = "${project.build.directory}/moditect")
    private File workingDirectory;

    @Parameter(readonly = true, defaultValue = "${project.build.directory}")
    private File buildDirectory;

    @Parameter(property = "outputDirectory", defaultValue = "${project.build.directory}/modules")
    private File outputDirectory;

    @Parameter(property = "overwriteExistingFiles", defaultValue = "false")
    private boolean overwriteExistingFiles;

    @Parameter
    private MainModuleConfiguration module;

    @Parameter
    private List<AddModuleConfiguration> modules;

    @Parameter
    private List<String> jdepsExtraArgs;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final File generatedSourceDirectory = new File( workingDirectory, "generated-sources" );
        final ArtifactResolutionHelper artifactResolutionHelper = new ArtifactResolutionHelper( repoSystem, repoSession,
                remoteRepos );
        final ModuleInfoGenerator moduleInfoGenerator = new ModuleInfoGenerator(new MavenArtifactResolver(project),
                artifactResolutionHelper,jdepsExtraArgs, new MojoLog(getLog()), workingDirectory, outputDirectory,
                modules);
        try {
            moduleInfoGenerator.add(generatedSourceDirectory,overwriteExistingFiles,module,buildDirectory,artifactId,
                    version);
        } catch (Exception e) {
            throw new MojoExecutionException("Problem generating module configuration",e);
        }
    }
}
