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
package org.moditect.mavenplugin.util;

import java.util.Iterator;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
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
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;
import org.moditect.mavenplugin.common.model.ArtifactConfiguration;
import org.moditect.mavenplugin.generate.CompileScopeDependencySelector;

public class ArtifactResolutionHelper {

    private RepositorySystem repoSystem;
    private RepositorySystemSession repoSession;
    private List<RemoteRepository> remoteRepos;

    public ArtifactResolutionHelper(RepositorySystem repoSystem, RepositorySystemSession repoSession,
                                    List<RemoteRepository> remoteRepos) {
        this.repoSystem = repoSystem;
        this.repoSession = repoSession;
        this.remoteRepos = remoteRepos;
    }

    public Artifact resolveArtifact(ArtifactConfiguration artifact) throws MojoExecutionException {
        return resolveArtifact(
                new DefaultArtifact(
                        artifact.getGroupId(),
                        artifact.getArtifactId(),
                        artifact.getClassifier(),
                        artifact.getType() != null ? artifact.getType() : "jar",
                        artifact.getVersion()));
    }

    public Artifact resolveArtifact(Artifact inputArtifact) throws MojoExecutionException {
        ArtifactRequest request = new ArtifactRequest();
        request.setArtifact(inputArtifact);
        request.setRepositories(remoteRepos);

        try {
            return repoSystem.resolveArtifact(repoSession, request).getArtifact();
        }
        catch (ArtifactResolutionException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    /**
     * Returns the dependencies applicable when compiling the given artifact (as far
     * as that's possible, if e.g. the POM of the dependency doesn't contain
     * specific dependencies used during compilation, we cannot retrieve them here).
     */
    public List<DependencyNode> getCompilationDependencies(Artifact inputArtifact) throws MojoExecutionException {
        try {
            CollectRequest collectRequest = new CollectRequest(new Dependency(inputArtifact, "compile"), remoteRepos);

            DefaultRepositorySystemSession sessionWithProvided = MavenRepositorySystemUtils.newSession();
            sessionWithProvided.setDependencySelector(
                    new AndDependencySelector(
                            new CompileScopeDependencySelector(),
                            new OptionalDependencySelector(),
                            new ExclusionDependencySelector()));
            sessionWithProvided.setLocalRepositoryManager(repoSession.getLocalRepositoryManager());

            CollectResult collectResult = repoSystem.collectDependencies(sessionWithProvided, collectRequest);

            PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
            collectResult.getRoot().accept(nlg);

            List<DependencyNode> dependencies = nlg.getNodes();

            // remove the input artifact itself
            Iterator<DependencyNode> it = dependencies.iterator();
            while (it.hasNext()) {
                DependencyNode next = it.next();
                if (next.getDependency() == collectRequest.getRoot()) {
                    it.remove();
                }
            }

            return dependencies;
        }
        catch (DependencyCollectionException e) {
            throw new MojoExecutionException("Couldn't collect dependencies of artifact " + inputArtifact, e);
        }
    }
}
