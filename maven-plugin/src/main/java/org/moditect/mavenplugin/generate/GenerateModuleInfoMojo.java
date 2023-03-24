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
package org.moditect.mavenplugin.generate;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.moditect.mavenplugin.common.model.ArtifactConfiguration;
import org.moditect.mavenplugin.common.model.ModuleInfoConfiguration;
import org.moditect.mavenplugin.generate.model.ArtifactIdentifier;
import org.moditect.mavenplugin.generate.model.ModuleConfiguration;
import org.moditect.mavenplugin.util.ArtifactResolutionHelper;

/**
 * @author Gunnar Morling
 */
@Mojo(name = "generate-module-info", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class GenerateModuleInfoMojo extends AbstractMojo {

    @Component
    private ArtifactHandlerManager artifactHandlerManager;

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;

    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    @Component
    private RepositorySystem repoSystem;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true, required = true)
    private RepositorySystemSession repoSession;

    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true, required = true)
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

    @Parameter(property = "moditect.skip", defaultValue = "false")
    private boolean skip;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        // Check if this plugin should be skipped
        if (skip) {
            getLog().debug("Mojo 'generate-module-info' skipped by configuration");
            return;
        }
        // Don't try to run this plugin, when packaging type is 'pom'
        // (may be better to only run it on specific packaging types, like 'jar')
        if (project.getModel().getPackaging().equalsIgnoreCase("pom")) {
            getLog().debug("Mojo 'generate-module-info' not executed on packaging type '" + project.getModel().getPackaging() + "'");
            return;
        }

        createDirectories();

        ArtifactResolutionHelper artifactResolutionHelper = new ArtifactResolutionHelper(repoSystem, repoSession, remoteRepos);
        ModuleInfoGenerator moduleInfoGenerator = new ModuleInfoGenerator(
                project, repoSystem, repoSession, remoteRepos, artifactResolutionHelper, jdepsExtraArgs, getLog(), workingDirectory, outputDirectory);

        Map<ArtifactIdentifier, String> assignedNamesByModule = getAssignedModuleNamesByModule(artifactResolutionHelper);

        if (artifactOverride != null) {
            ModuleConfiguration moduleConfiguration = getModuleConfigurationFromOverrides();
            moduleInfoGenerator.generateModuleInfo(
                    moduleConfiguration.getArtifact(),
                    moduleConfiguration.getAdditionalDependencies(),
                    moduleConfiguration.getModuleInfo(),
                    assignedNamesByModule,
                    Collections.emptyMap());
        }
        else {
            for (ModuleConfiguration moduleConfiguration : modules) {
                moduleInfoGenerator.generateModuleInfo(
                        moduleConfiguration.getArtifact(),
                        moduleConfiguration.getAdditionalDependencies(),
                        moduleConfiguration.getModuleInfo(),
                        assignedNamesByModule,
                        Collections.emptyMap());
            }
        }
    }

    private Map<ArtifactIdentifier, String> getAssignedModuleNamesByModule(ArtifactResolutionHelper artifactResolutionHelper) throws MojoExecutionException {
        Map<ArtifactIdentifier, String> assignedNamesByModule = new HashMap<>();

        for (ModuleConfiguration configuredModule : modules) {
            assignedNamesByModule.put(
                    new ArtifactIdentifier(artifactResolutionHelper.resolveArtifact(configuredModule.getArtifact())),
                    configuredModule.getModuleInfo().getName());
        }

        return assignedNamesByModule;
    }

    private ModuleConfiguration getModuleConfigurationFromOverrides() {
        ModuleConfiguration moduleConfiguration = new ModuleConfiguration();

        moduleConfiguration.setArtifact(new ArtifactConfiguration(artifactOverride));
        moduleConfiguration.setModuleInfo(new ModuleInfoConfiguration());
        moduleConfiguration.getModuleInfo().setName(moduleNameOverride);

        if (additionalDependenciesOverride != null) {
            for (String additionalDependency : additionalDependenciesOverride.split("\\,")) {
                moduleConfiguration.getAdditionalDependencies().add(new ArtifactConfiguration(additionalDependency));
            }
        }

        if (exportExcludesOverride != null) {
            moduleConfiguration.getModuleInfo().setExports(exportExcludesOverride);
        }

        moduleConfiguration.getModuleInfo().setAddServiceUses(addServiceUsesOverride);

        return moduleConfiguration;
    }

    private void createDirectories() {
        if (!workingDirectory.exists()) {
            workingDirectory.mkdirs();
        }

        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }
    }
}
