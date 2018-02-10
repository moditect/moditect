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
package org.moditect.mavenplugin.internal;

import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.moditect.commands.AddModuleInfo;
import org.moditect.dependency.ArtifactResolver;
import org.moditect.generator.ModuleInfoGenerator;
import org.moditect.model.DependencyDescriptor;
import org.moditect.model.add.MainModuleConfiguration;
import org.moditect.model.common.ArtifactConfiguration;
import org.moditect.model.generate.ArtifactIdentifier;
import org.moditect.spi.log.Log;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Maven related tools specific resolvers.
 *
 * @author Pratik Parikh
 */
public class MavenArtifactResolver implements ArtifactResolver {

    final MavenProject project;

    public MavenArtifactResolver(final MavenProject project){
        this.project = project;
    }

    @Override
    public String getVersionFromProject(Artifact artifact) throws Exception {
        Optional<org.apache.maven.artifact.Artifact> resolvedDependency = project.getArtifacts()
                .stream()
                .filter( a -> {
                    return Objects.equals( a.getGroupId(), artifact.getGroupId() ) &&
                            Objects.equals( a.getArtifactId(), artifact.getArtifactId() ) &&
                            ModuleInfoGenerator.areEqualClassifiers( a.getClassifier(), artifact.getClassifier() ) &&
                            Objects.equals( a.getType(), artifact.getExtension() );
                } )
                .findFirst();

        if ( resolvedDependency.isPresent() ) {
            return resolvedDependency.get().getVersion();
        }

        Optional<org.apache.maven.model.Dependency> managed = project.getDependencyManagement()
                .getDependencies()
                .stream()
                .filter( d -> {
                    return Objects.equals( d.getGroupId(), artifact.getGroupId() ) &&
                            Objects.equals( d.getArtifactId(), artifact.getArtifactId() ) &&
                            ModuleInfoGenerator.areEqualClassifiers( d.getClassifier(), artifact.getClassifier() ) &&
                            Objects.equals( d.getType(), artifact.getExtension() );
                } )
                .findFirst();

        if ( managed.isPresent() ) {
            return managed.get().getVersion();
        }

        return null;
    }

    @Override
    public DefaultRepositorySystemSession newSession() {
        return MavenRepositorySystemUtils.newSession();
    }

    @Override
    public Set<DependencyDescriptor> getDependencyDescriptors(
            final Map<ArtifactIdentifier, String> assignedNamesByModule) {
        final Set<DependencyDescriptor> dependencies = project.getArtifacts().stream()
                .map( d -> new DependencyDescriptor(
                                d.getFile().toPath(),
                                d.isOptional(),
                                getAssignedModuleName( assignedNamesByModule, d )
                        )
                )
                .collect( Collectors.toSet() );;
        return dependencies;
    }

    @Override
    public String determineVersion(ArtifactConfiguration artifact) throws Exception {
        Optional<org.apache.maven.artifact.Artifact> resolvedDependency = project.getArtifacts()
                .stream()
                .filter( a -> {
                    return Objects.equals( a.getGroupId(), artifact.getGroupId() ) &&
                            Objects.equals( a.getArtifactId(), artifact.getArtifactId() ) &&
                            Objects.equals( a.getClassifier(), artifact.getClassifier() ) &&
                            Objects.equals( a.getType(), artifact.getType() );
                } )
                .findFirst();

        if ( resolvedDependency.isPresent() ) {
            return resolvedDependency.get().getVersion();
        }

        Optional<org.apache.maven.model.Dependency> managed = project.getDependencyManagement()
                .getDependencies()
                .stream()
                .filter( d -> {
                    return Objects.equals( d.getGroupId(), artifact.getGroupId() ) &&
                            Objects.equals( d.getArtifactId(), artifact.getArtifactId() ) &&
                            Objects.equals( d.getClassifier(), artifact.getClassifier() ) &&
                            Objects.equals( d.getType(), artifact.getType() );
                } )
                .findFirst();

        if ( managed.isPresent() ) {
            return managed.get().getVersion();
        }

        else {
            throw new Exception(
                    "A version must be given for artifact " + artifact.toDependencyString()
                            + ". Either specify one explicitly, add it to the project dependencies"
                            + " or add it to the project's dependency management."
            );
        }
    }

    @Override
    public Path getSourceDirectory(Path defaultDirectory) {
        Optional<Path> defaultSourceDirectory = null;

        final List<Path> sourceDirectories = project.getCompileSourceRoots().stream().filter(directoryStr -> {
            return (new File(directoryStr)).exists();
        }).map(directoryStr -> new File(directoryStr).toPath()).collect(Collectors.toList());

        if(sourceDirectories.size() == 1)
        {
            defaultSourceDirectory = Optional.of(sourceDirectories.get(0));
        } else {
            defaultSourceDirectory = sourceDirectories.stream().filter(currentDirectory -> {
                return currentDirectory.toString().endsWith("/src/main/java");
            }).findFirst();
        }

        Path sourceFile = null;

        if(defaultSourceDirectory.isPresent()){
            sourceFile = defaultSourceDirectory.get();
        } else if(defaultDirectory != null) {
            sourceFile = defaultDirectory;
        }else if(sourceDirectories.size() > 1) {
            sourceFile = sourceDirectories.get(0);
        }

        return sourceFile;
    }

    @Override
    public void postActivity(AddModuleInfo addModuleInfo, MainModuleConfiguration module,
                             ModuleInfoGenerator moduleInfoGenerator,
                             Map<ArtifactIdentifier, String> assignedNamesByModule,
                             Map<ArtifactIdentifier, Path> modularizedJars) throws Exception {
        // do nothing.
    }

    private String getAssignedModuleName(Map<ArtifactIdentifier, String> assignedNamesByModule,
                                         org.apache.maven.artifact.Artifact artifact) {
        for ( Map.Entry<ArtifactIdentifier, String> assignedNameByModule : assignedNamesByModule.entrySet() ) {
            // ignoring the version; the resolved artifact could have a different version then the one used
            // in this modularization build
            if ( assignedNameByModule.getKey().getGroupId().equals( artifact.getGroupId() ) &&
                    assignedNameByModule.getKey().getArtifactId().equals( artifact.getArtifactId() ) &&
                    ( artifact.getClassifier() == null && assignedNameByModule.getKey().getClassifier().equals("") || assignedNameByModule.getKey().getClassifier().equals( artifact.getClassifier() ) ) &&
                    assignedNameByModule.getKey().getExtension().equals( artifact.getType() ) ) {
                return assignedNameByModule.getValue();
            }
        }

        return null;
    }
}
