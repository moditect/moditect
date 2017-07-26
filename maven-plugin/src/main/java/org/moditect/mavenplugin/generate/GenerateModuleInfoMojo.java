/**
 *  Copyright 2017 Gunnar Morling (http://www.gunnarmorling.de/)
 *  and/or other contributors as indicated by the @authors tag. See the
 *  copyright.txt file in the distribution for a full listing of all
 *  contributors.
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.artifact.resolve.ArtifactResolver;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.util.graph.selector.AndDependencySelector;
import org.eclipse.aether.util.graph.selector.ExclusionDependencySelector;
import org.eclipse.aether.util.graph.selector.OptionalDependencySelector;
import org.eclipse.aether.util.graph.selector.ScopeDependencySelector;
import org.moditect.commands.GenerateModuleInfo;
import org.moditect.mavenplugin.common.model.ArtifactConfiguration;
import org.moditect.mavenplugin.generate.model.ModuleConfiguration;
import org.moditect.mavenplugin.util.MojoLog;
import org.moditect.model.DependencyDescriptor;
import org.moditect.model.PackageNamePattern;

/**
 * @author Gunnar Morling
 */
@Mojo(name = "generate-module-info", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class GenerateModuleInfoMojo extends AbstractMojo {

    @Component
    private ArtifactResolver artifactResolver;

    @Component
    private ArtifactHandlerManager artifactHandlerManager;

    @Parameter( defaultValue = "${session}", readonly = true, required = true )
    private MavenSession session;

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
        createDirectories();

        if ( artifactOverride != null ) {
            processModule( getModuleConfigurationFromOverrides() );
        }
        else {
            for ( ModuleConfiguration moduleConfiguration : modules ) {
                processModule( moduleConfiguration );
            }
        }
    }

    private ModuleConfiguration getModuleConfigurationFromOverrides() {
        ModuleConfiguration moduleConfiguration = new ModuleConfiguration();

        moduleConfiguration.setArtifact( new ArtifactConfiguration( artifactOverride ) );
        moduleConfiguration.setModuleName( moduleNameOverride );

        if ( additionalDependenciesOverride != null ) {
            for ( String additionalDependency : additionalDependenciesOverride.split( "\\," ) ) {
                moduleConfiguration.getAdditionalDependencies().add( new ArtifactConfiguration( additionalDependency ) );
            }
        }

        if ( exportExcludesOverride != null ) {
            moduleConfiguration.setExports( exportExcludesOverride );
        }

        moduleConfiguration.setAddServiceUses( addServiceUsesOverride );

        return moduleConfiguration;
    }

    private void processModule(ModuleConfiguration moduleConfiguration) throws MojoExecutionException {
        Artifact inputArtifact = resolveArtifact(
            new DefaultArtifact( moduleConfiguration.getArtifact().toDependencyString() )
        );

        Set<DependencyDescriptor> dependencies = getDependencies( inputArtifact );

        for( ArtifactConfiguration further : moduleConfiguration.getAdditionalDependencies() ) {
            Artifact furtherArtifact = resolveArtifact( new DefaultArtifact( further.toDependencyString() ) );
            dependencies.add( new DependencyDescriptor( furtherArtifact.getFile().toPath(), false, null ) );
        }

        List<String> requireOverrides;

        if ( moduleConfiguration.getRequireOverrides() != null ) {
            requireOverrides = Arrays.stream( moduleConfiguration.getRequireOverrides().trim().split(";") )
                .map( String::trim )
                .map( r -> "requires " + r )
                .collect( Collectors.toList() );
        }
        else {
            requireOverrides = Collections.emptyList();
        }

        new GenerateModuleInfo(
                inputArtifact.getFile().toPath(),
                moduleConfiguration.getModuleName(),
                dependencies,
                PackageNamePattern.parsePatterns( moduleConfiguration.getExports() ),
                requireOverrides,
                workingDirectory.toPath(),
                outputDirectory.toPath(),
                moduleConfiguration.isAddServiceUses(),
                new MojoLog( getLog() )
        )
        .run();
    }

    private Set<DependencyDescriptor> getDependencies(Artifact inputArtifact) throws MojoExecutionException {
        CollectRequest collectRequest = new CollectRequest( new Dependency( inputArtifact, "provided" ), remoteRepos );
        CollectResult collectResult = null;

        try {
            RepositorySystemSession sessionWithProvided = new DefaultRepositorySystemSession( repoSession )
                .setDependencySelector(
                    new AndDependencySelector(
                        new ScopeDependencySelector( "test" ),
                        new OptionalDependencySelector(),
                        new ExclusionDependencySelector()
                    )
                );

            collectResult = repoSystem.collectDependencies( sessionWithProvided, collectRequest );
        }
        catch (DependencyCollectionException e) {
            throw new MojoExecutionException( "Couldn't collect dependencies", e );
        }

        Set<DependencyDescriptor> dependencies = new LinkedHashSet<>();
        Map<String, Artifact> resolvedModules = new HashMap<>();

        for ( ModuleConfiguration configuredModule : modules ) {
            resolvedModules.put(
                configuredModule.getModuleName(),
                resolveArtifact( new DefaultArtifact( configuredModule.getArtifact().toDependencyString() ) )
            );
        }

        for ( DependencyNode dependency : collectResult.getRoot().getChildren() ) {
            Artifact resolvedDependency = resolveArtifact( dependency.getDependency().getArtifact() );
            String assignedModuleName = null;
            for ( Entry<String, Artifact> resolvedModule : resolvedModules.entrySet() ) {
                if ( resolvedModule.getValue().getFile().toPath().equals( resolvedDependency.getFile().toPath() ) ) {
                    assignedModuleName = resolvedModule.getKey();
                }
            }

            dependencies.add(
                    new DependencyDescriptor(
                            resolvedDependency.getFile().toPath(),
                            dependency.getDependency().isOptional(),
                            assignedModuleName
                    )
            );
        }

        return dependencies;
    }

    private void createDirectories() {
        if ( !workingDirectory.exists() ) {
            workingDirectory.mkdirs();
        }

        if ( !outputDirectory.exists() ) {
            outputDirectory.mkdirs();
        }
    }

    private Artifact resolveArtifact(Artifact inputArtifact) throws MojoExecutionException {
        ArtifactRequest request = new ArtifactRequest();
        request.setArtifact( inputArtifact );
        request.setRepositories( remoteRepos );

        try {
            return repoSystem.resolveArtifact( repoSession, request ).getArtifact();
        }
        catch (ArtifactResolutionException e) {
            throw new MojoExecutionException( e.getMessage(), e );
        }
    }
}
