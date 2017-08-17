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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.util.graph.selector.AndDependencySelector;
import org.eclipse.aether.util.graph.selector.ExclusionDependencySelector;
import org.eclipse.aether.util.graph.selector.OptionalDependencySelector;
import org.eclipse.aether.util.graph.selector.ScopeDependencySelector;
import org.moditect.commands.GenerateModuleInfo;
import org.moditect.mavenplugin.common.model.ArtifactConfiguration;
import org.moditect.mavenplugin.common.model.ModuleInfoConfiguration;
import org.moditect.mavenplugin.generate.model.ArtifactIdentifier;
import org.moditect.mavenplugin.util.ArtifactResolutionHelper;
import org.moditect.mavenplugin.util.MojoLog;
import org.moditect.model.DependencePattern;
import org.moditect.model.DependencyDescriptor;
import org.moditect.model.GeneratedModuleInfo;
import org.moditect.model.PackageNamePattern;

public class ModuleInfoGenerator {

    private RepositorySystem repoSystem;
    private RepositorySystemSession repoSession;
    private List<RemoteRepository> remoteRepos;
    private ArtifactResolutionHelper artifactResolutionHelper;
    private Log log;
    private File workingDirectory;
    private File outputDirectory;

    public ModuleInfoGenerator(RepositorySystem repoSystem, RepositorySystemSession repoSession,
            List<RemoteRepository> remoteRepos, ArtifactResolutionHelper artifactResolutionHelper, Log log, File workingDirectory, File outputDirectory) {
        this.repoSystem = repoSystem;
        this.repoSession = repoSession;
        this.remoteRepos = remoteRepos;
        this.artifactResolutionHelper = artifactResolutionHelper;
        this.log = log;
        this.workingDirectory = workingDirectory;
        this.outputDirectory = outputDirectory;
    }

    public GeneratedModuleInfo generateModuleInfo(ArtifactConfiguration artifact, List<ArtifactConfiguration> additionalDependencies, ModuleInfoConfiguration moduleInfo, Map<ArtifactIdentifier, String> assignedNamesByModule) throws MojoExecutionException {
        Artifact inputArtifact = artifactResolutionHelper.resolveArtifact(artifact);

        Set<DependencyDescriptor> dependencies = getDependencies( inputArtifact, assignedNamesByModule );

        for( ArtifactConfiguration further : additionalDependencies ) {
            Artifact furtherArtifact = artifactResolutionHelper.resolveArtifact( further );
            dependencies.add( new DependencyDescriptor( furtherArtifact.getFile().toPath(), false, null ) );
        }

        Set<String> uses;

        if ( moduleInfo.getUses() != null ) {
            uses = Arrays.stream( moduleInfo.getUses().split( ";" ) )
                .map( String::trim )
                .collect( Collectors.toSet() );
        }
        else {
            uses = Collections.emptySet();
        }

        return new GenerateModuleInfo(
                inputArtifact.getFile().toPath(),
                moduleInfo.getName(),
                dependencies,
                PackageNamePattern.parsePatterns( moduleInfo.getExports() ),
                PackageNamePattern.parsePatterns( moduleInfo.getOpens() ),
                DependencePattern.parsePatterns( moduleInfo.getRequires() ),
                workingDirectory.toPath(),
                outputDirectory.toPath(),
                uses,
                moduleInfo.isAddServiceUses(),
                new MojoLog( log )
        )
        .run();
    }

    private Set<DependencyDescriptor> getDependencies(Artifact inputArtifact, Map<ArtifactIdentifier, String> assignedNamesByModule) throws MojoExecutionException {
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

        for ( DependencyNode dependency : collectResult.getRoot().getChildren() ) {
            Artifact resolvedDependency = artifactResolutionHelper.resolveArtifact( dependency.getDependency().getArtifact() );
            String assignedModuleName = getAssignedModuleName( assignedNamesByModule, new ArtifactIdentifier( resolvedDependency ) );

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

    private String getAssignedModuleName(Map<ArtifactIdentifier, String> assignedNamesByModule, ArtifactIdentifier artifactIdentifier) {
        for ( Entry<ArtifactIdentifier, String> assignedNameByModule : assignedNamesByModule.entrySet() ) {
            // ignoring the version; the resolved artifact could have a different version then the one used
            // in this modularization build
            if ( assignedNameByModule.getKey().getGroupId().equals( artifactIdentifier.getGroupId() ) &&
                    assignedNameByModule.getKey().getArtifactId().equals( artifactIdentifier.getArtifactId() ) &&
                    assignedNameByModule.getKey().getClassifier().equals( artifactIdentifier.getClassifier() ) &&
                    assignedNameByModule.getKey().getExtension().equals( artifactIdentifier.getExtension() ) ) {
                return assignedNameByModule.getValue();
            }
        }

        return null;
    }
}
