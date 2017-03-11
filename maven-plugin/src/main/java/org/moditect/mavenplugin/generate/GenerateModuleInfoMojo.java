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
import java.util.ArrayList;
import java.util.List;

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
import org.moditect.commands.model.DependencyDescriptor;
import org.moditect.mavenplugin.common.model.ArtifactConfiguration;
import org.moditect.mavenplugin.generate.model.ModuleConfiguration;

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

//    @Parameter(property = "moditect.artifact")
//    private String artifactOverride;

//    private String artifact = "io.undertow:undertow-core:1.4.11.Final";
//    private String furtherArtifacts = "org.jboss.logging:jboss-logging-annotations:2.0.1.Final";

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        createDirectories();

//        String artifactCoordinates;

//        if ( artifactOverride != null ) {
//            artifactCoordinates = artifactOverride;
//        }
//        else if ( artifact != null ){
//            artifactCoordinates = artifact.toDependencyString();
//        }
//        else {
//            throw new MojoExecutionException( "An input artifact must be specified either via <artifact> in the plug-in configuration or using the property \"moditect.artifact\"" );
//        }

        for ( ModuleConfiguration moduleConfiguration : modules ) {
            Artifact inputArtifact = resolveArtifact(
                new DefaultArtifact( moduleConfiguration.getArtifact().toDependencyString() )
            );

            List<DependencyDescriptor> dependencies = getDependencies( inputArtifact );

            for( ArtifactConfiguration further : moduleConfiguration.getAdditionalDependencies() ) {
                Artifact furtherArtifact = resolveArtifact( new DefaultArtifact( further.toDependencyString() ) );
                dependencies.add( new DependencyDescriptor( furtherArtifact.getFile().toPath() ) );
            }

            new GenerateModuleInfo(
                    inputArtifact.getFile().toPath(),
                    moduleConfiguration.getModuleName(),
                    dependencies,
                    workingDirectory.toPath(),
                    outputDirectory.toPath()
            )
            .run();
        }
    }

    private List<DependencyDescriptor> getDependencies(Artifact inputArtifact) throws MojoExecutionException {
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

        List<DependencyDescriptor> dependencies = new ArrayList<>();

        for ( DependencyNode dependency : collectResult.getRoot().getChildren() ) {
            Artifact resolvedDependency = resolveArtifact( dependency.getDependency().getArtifact() );
            dependencies.add( new DependencyDescriptor( resolvedDependency.getFile().toPath() ) );
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
