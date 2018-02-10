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
package org.moditect.gradleplugin.internal;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.moditect.commands.AddModuleInfo;
import org.moditect.dependency.ArtifactResolver;
import org.moditect.generator.ArtifactResolutionHelper;
import org.moditect.generator.ModuleInfoGenerator;
import org.moditect.model.DependencyDescriptor;
import org.moditect.model.add.MainModuleConfiguration;
import org.moditect.model.common.ArtifactConfiguration;
import org.moditect.model.generate.ArtifactIdentifier;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Gradle related tools specific resolvers.
 *
 * @author Pratik Parikh
 */
public class GradleArtifactResolver implements ArtifactResolver {

    final Project project;

    public GradleArtifactResolver(final Project project){
        this.project = project;
    }

    @Override
    public String getVersionFromProject(Artifact artifact) throws Exception {
        final ArtifactConfiguration artifactConfiguration = new ArtifactConfiguration();
        artifactConfiguration.setVersion(artifact.getVersion());
        artifactConfiguration.setType(artifact.getExtension());
        artifactConfiguration.setGroupId(artifact.getGroupId());
        artifactConfiguration.setArtifactId(artifact.getArtifactId());
        artifactConfiguration.setClassifier(artifact.getClassifier());
        artifactConfiguration.setDependencyString(artifactConfiguration.toDependencyString());
        return determineVersion(artifactConfiguration);
    }

    @Override
    public DefaultRepositorySystemSession newSession() {
        return MavenRepositorySystemUtils.newSession();
    }

    private String getAssignedModuleName(Map<ArtifactIdentifier, String> assignedNamesByModule,
                                         Artifact artifact) {
        String module = null;
        for ( Map.Entry<ArtifactIdentifier, String> assignedNameByModule : assignedNamesByModule.entrySet() ) {
            // ignoring the version; the resolved artifact could have a different version then the one used
            // in this modularization build
            if ( assignedNameByModule.getKey().getGroupId().equals( artifact.getGroupId() ) &&
                    assignedNameByModule.getKey().getArtifactId().equals( artifact.getArtifactId() ) &&
                    assignedNameByModule.getKey().getExtension().equals( artifact.getExtension() ) ) {
                module = assignedNameByModule.getValue();
                break;
            }
        }

        return module;
    }

    @Override
    public Set<DependencyDescriptor> getDependencyDescriptors(
            final Map<ArtifactIdentifier, String> assignedNamesByModule) {
        final Set<DependencyDescriptor> dependencies = project.getConfigurations().getByName("compile").
                getResolvedConfiguration().getResolvedArtifacts().stream().map(a ->
            new DependencyDescriptor(
                    a.getFile().toPath(),
                    false,
                    getAssignedModuleName(assignedNamesByModule,
                            new DefaultArtifact(a.getModuleVersion().getId().getGroup(), a.getName(),
                                    a.getModuleVersion().getId().getVersion(),a.getExtension(),a.getType())))
        ).collect(Collectors.toSet());
        return dependencies;
    }

    @Override
    public String determineVersion(ArtifactConfiguration artifact) throws Exception {

        if(artifact.getVersion() != null){
            return artifact.getVersion();
        }

        final Optional<ResolvedArtifact> resolvedDependency = project.getConfigurations().getByName("compile").
                getResolvedConfiguration().getResolvedArtifacts().stream().filter(a -> {
            final ModuleVersionIdentifier id = a.getModuleVersion().getId();
                    return Objects.equals( id.getGroup(), artifact.getGroupId() ) &&
                            Objects.equals( id.getName(), artifact.getArtifactId() )&&
                                    ModuleInfoGenerator.areEqualClassifiers( a.getClassifier(), artifact.getClassifier() ) &&
                                    Objects.equals( a.getType(), artifact.getType() );
                }).findFirst();
        if ( resolvedDependency.isPresent() ) {
            return resolvedDependency.get().getModuleVersion().getId().getVersion();
        } else {
            throw new Exception(
                    "A version must be given for artifact " + artifact.toDependencyString()
                            + ". Either specify one explicitly, add it to the project dependencies"
                            + " or add it to the project's dependency management."
            );
        }
    }

    @Override
    public Path getSourceDirectory(Path defaultDirectory) {
        final JavaPluginConvention javaConvention =
                project.getConvention().getPlugin(JavaPluginConvention.class);
        final SourceSet mainSourceSet = javaConvention.getSourceSets().getByName(
                SourceSet.MAIN_SOURCE_SET_NAME);
        Optional<Path> defaultSourceDirectory = null;

        final List<Path> sourceDirectories = mainSourceSet.getAllJava().getSrcDirs().stream().filter(directoryFile -> {
            return directoryFile.exists();
        }).map(directoryFile -> directoryFile.toPath()).collect(Collectors.toList());

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
        try {
            project.getTasks().findByName("generateModuleInfo").getExtensions().add("finalizeAddModuleInfo",
                    addModuleInfo);
        } catch (Exception e) {
            throw new Exception("Project is not build so generate the module-info.java, if does not exist!",e);
        }
    }

    protected RepositorySystem newRepositorySystem() {
        final DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, FileTransporterFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);

        locator.setErrorHandler(new DefaultServiceLocator.ErrorHandler() {
            @Override
            public void serviceCreationFailed(Class<?> type, Class<?> impl, Throwable exception) {
                exception.printStackTrace();
            }
        });

        return locator.getService(RepositorySystem.class);
    }

    protected DefaultRepositorySystemSession newRepositorySystemSession(Project project, RepositorySystem system) {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

        File dir = new File(project.getBuildDir(), "aether/repository");
        LocalRepository localRepo = new LocalRepository(dir);
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));

        return session;
    }

    public ArtifactResolutionHelper getArtifactResolutionHelper(){
        final RepositorySystem repoSystem = newRepositorySystem();
        final RepositorySystemSession repoSession = newRepositorySystemSession(project,repoSystem);
        final List<RemoteRepository> remoteRepos =  project.getRepositories().stream()
                .map(repository -> new RemoteRepository.Builder(((MavenArtifactRepository)repository).getName()
                , "default", ((MavenArtifactRepository)repository).getUrl().toString()).build())
                .collect(Collectors.toList());

        remoteRepos.addAll(project.getRootProject().getRepositories().stream()
                .map(repository -> new RemoteRepository.Builder(((MavenArtifactRepository)repository).getName()
                , "default", ((MavenArtifactRepository)repository).getUrl().toString()).build())
                .collect(Collectors.toList()));

        return  new ArtifactResolutionHelper( repoSystem, repoSession, remoteRepos );
    }
}
