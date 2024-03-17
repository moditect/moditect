/*
 *  SPDX-License-Identifier: Apache-2.0
 *
 *   Copyright The original authors
 *
 *   Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.moditect.mavenplugin.generate;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.RemoteRepository;
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

    private final MavenProject project;
    private final RepositorySystem repoSystem;
    private final RepositorySystemSession repoSession;
    private final List<RemoteRepository> remoteRepos;
    private final ArtifactResolutionHelper artifactResolutionHelper;
    private final List<String> jdepsExtraArgs;
    private final Log log;
    private final File workingDirectory;
    private final File outputDirectory;

    public ModuleInfoGenerator(MavenProject project, RepositorySystem repoSystem, RepositorySystemSession repoSession,
                               List<RemoteRepository> remoteRepos, ArtifactResolutionHelper artifactResolutionHelper, List<String> jdepsExtraArgs, Log log,
                               File workingDirectory, File outputDirectory) {
        this.project = project;
        this.repoSystem = repoSystem;
        this.repoSession = repoSession;
        this.remoteRepos = remoteRepos;
        this.artifactResolutionHelper = artifactResolutionHelper;
        this.jdepsExtraArgs = jdepsExtraArgs;
        this.log = log;
        this.workingDirectory = workingDirectory;
        this.outputDirectory = outputDirectory;
    }

    public GeneratedModuleInfo generateModuleInfo(ArtifactConfiguration artifact, List<ArtifactConfiguration> additionalDependencies, ModuleInfoConfiguration moduleInfo,
                                                  Map<ArtifactIdentifier, String> assignedNamesByModule, Map<ArtifactIdentifier, Path> modularizedJars)
            throws MojoExecutionException {
        log.debug("Adding module descriptor to artifact " + artifact.toDependencyString());

        Artifact inputArtifact = artifactResolutionHelper.resolveArtifact(artifact);

        Set<DependencyDescriptor> dependencies = getDependencies(inputArtifact, assignedNamesByModule, modularizedJars);

        for (ArtifactConfiguration further : additionalDependencies) {
            Artifact furtherArtifact = artifactResolutionHelper.resolveArtifact(further);
            Path modularized = getModularizedJar(modularizedJars,
                    new ArtifactIdentifier(further.getGroupId(), further.getArtifactId(), further.getVersion(), further.getType(), further.getClassifier()));
            dependencies.add(new DependencyDescriptor(modularized != null ? modularized : furtherArtifact.getFile().toPath(), false, null));
        }

        return generateModuleInfo(inputArtifact.getFile().toPath(), dependencies, moduleInfo);
    }

    public GeneratedModuleInfo generateModuleInfo(Path inputJar, List<ArtifactConfiguration> additionalDependencies, ModuleInfoConfiguration moduleInfo,
                                                  Map<ArtifactIdentifier, String> assignedNamesByModule)
            throws MojoExecutionException {
        Set<DependencyDescriptor> dependencies = new HashSet<>();

        for (ArtifactConfiguration further : additionalDependencies) {
            Artifact furtherArtifact = artifactResolutionHelper.resolveArtifact(further);
            dependencies.add(new DependencyDescriptor(furtherArtifact.getFile().toPath(), false, null));
        }

        return generateModuleInfo(inputJar, dependencies, moduleInfo);
    }

    public GeneratedModuleInfo generateModuleInfo(Path inputJar, Set<DependencyDescriptor> dependencies, ModuleInfoConfiguration moduleInfo)
            throws MojoExecutionException {
        Set<String> opensResources;

        if (moduleInfo.getOpensResources() != null) {
            opensResources = Arrays.stream(moduleInfo.getOpensResources().split(";"))
                    .map(String::trim)
                    .collect(Collectors.toSet());
        }
        else {
            opensResources = Collections.emptySet();
        }

        Set<String> uses;

        if (moduleInfo.getUses() != null) {
            uses = Arrays.stream(moduleInfo.getUses().split(";"))
                    .map(String::trim)
                    .collect(Collectors.toSet());
        }
        else {
            uses = Collections.emptySet();
        }

        Set<String> provides;

        if (moduleInfo.getProvides() != null) {
            provides = Arrays.stream(moduleInfo.getProvides().split(";"))
                    .map(String::trim)
                    .collect(Collectors.toSet());
        }
        else {
            provides = Collections.emptySet();
        }

        return new GenerateModuleInfo(
                inputJar,
                moduleInfo.getName(),
                moduleInfo.isOpen(),
                dependencies,
                PackageNamePattern.parsePatterns(moduleInfo.getExports()),
                PackageNamePattern.parsePatterns(moduleInfo.getOpens()),
                DependencePattern.parsePatterns(moduleInfo.getRequires()),
                workingDirectory.toPath(),
                outputDirectory.toPath(),
                opensResources,
                uses,
                provides,
                moduleInfo.isAddServiceUses(),
                jdepsExtraArgs,
                new MojoLog(log))
                .run();
    }

    private Set<DependencyDescriptor> getDependencies(Artifact inputArtifact, Map<ArtifactIdentifier, String> assignedNamesByModule,
                                                      Map<ArtifactIdentifier, Path> modularizedJars)
            throws MojoExecutionException {
        Set<DependencyDescriptor> dependencies = new LinkedHashSet<>();

        for (DependencyNode dependency : artifactResolutionHelper.getCompilationDependencies(inputArtifact)) {
            Artifact artifact = dependency.getDependency().getArtifact();

            // use the version of the dependency as used within the current project's build, if present
            String versionFromProject = getVersionFromProject(artifact);
            if (versionFromProject != null) {
                artifact = new DefaultArtifact(artifact.getGroupId(), artifact.getArtifactId(), artifact.getClassifier(), artifact.getExtension(), versionFromProject);
            }

            Artifact resolvedDependency = artifactResolutionHelper.resolveArtifact(artifact);
            String assignedModuleName = getAssignedModuleName(assignedNamesByModule, new ArtifactIdentifier(resolvedDependency));
            Path modularized = getModularizedJar(modularizedJars, new ArtifactIdentifier(resolvedDependency));

            dependencies.add(
                    new DependencyDescriptor(
                            modularized != null ? modularized : resolvedDependency.getFile().toPath(),
                            dependency.getDependency().isOptional(),
                            assignedModuleName));
        }

        return dependencies;
    }

    private String getAssignedModuleName(Map<ArtifactIdentifier, String> assignedNamesByModule, ArtifactIdentifier artifactIdentifier) {
        for (Entry<ArtifactIdentifier, String> assignedNameByModule : assignedNamesByModule.entrySet()) {
            // ignoring the version; the resolved artifact could have a different version then the one used
            // in this modularization build
            if (assignedNameByModule.getKey().getGroupId().equals(artifactIdentifier.getGroupId()) &&
                    assignedNameByModule.getKey().getArtifactId().equals(artifactIdentifier.getArtifactId()) &&
                    assignedNameByModule.getKey().getClassifier().equals(artifactIdentifier.getClassifier()) &&
                    assignedNameByModule.getKey().getExtension().equals(artifactIdentifier.getExtension())) {
                return assignedNameByModule.getValue();
            }
        }

        return null;
    }

    private Path getModularizedJar(Map<ArtifactIdentifier, Path> modularizedJars, ArtifactIdentifier artifactIdentifier) {
        for (Entry<ArtifactIdentifier, Path> assignedNameByModule : modularizedJars.entrySet()) {
            // ignoring the version; the resolved artifact could have a different version than the one used
            // in this modularization build
            if (assignedNameByModule.getKey().getGroupId().equals(artifactIdentifier.getGroupId()) &&
                    assignedNameByModule.getKey().getArtifactId().equals(artifactIdentifier.getArtifactId()) &&
                    areEqualClassifiers(assignedNameByModule.getKey().getClassifier(), artifactIdentifier.getClassifier()) &&
                    assignedNameByModule.getKey().getExtension().equals(artifactIdentifier.getExtension())) {
                return assignedNameByModule.getValue();
            }
        }

        return null;
    }

    private String getVersionFromProject(Artifact artifact) throws MojoExecutionException {
        Optional<org.apache.maven.artifact.Artifact> resolvedDependency = project.getArtifacts()
                .stream()
                .filter(a -> {
                    return Objects.equals(a.getGroupId(), artifact.getGroupId()) &&
                            Objects.equals(a.getArtifactId(), artifact.getArtifactId()) &&
                            areEqualClassifiers(a.getClassifier(), artifact.getClassifier()) &&
                            Objects.equals(a.getType(), artifact.getExtension());
                })
                .findFirst();

        if (resolvedDependency.isPresent()) {
            return resolvedDependency.get().getVersion();
        }

        if (project.getDependencyManagement() != null) {
            Optional<org.apache.maven.model.Dependency> managed = project.getDependencyManagement()
                    .getDependencies()
                    .stream()
                    .filter(d -> {
                        return Objects.equals(d.getGroupId(), artifact.getGroupId()) &&
                                Objects.equals(d.getArtifactId(), artifact.getArtifactId()) &&
                                areEqualClassifiers(d.getClassifier(), artifact.getClassifier()) &&
                                Objects.equals(d.getType(), artifact.getExtension());
                    })
                    .findFirst();

            if (managed.isPresent()) {
                return managed.get().getVersion();
            }
        }

        return null;
    }

    private boolean areEqualClassifiers(String classifier1, String classifier2) {
        if (classifier1 != null && classifier1.isEmpty()) {
            classifier1 = null;
        }
        if (classifier2 != null && classifier2.isEmpty()) {
            classifier2 = null;
        }

        return Objects.equals(classifier1, classifier2);
    }
}
