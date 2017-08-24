/**
 *  Copyright 2017 The ModiTect authors
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

import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.moditect.mavenplugin.common.model.ArtifactConfiguration;

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
                        artifact.getVersion()
                )
            );
    }

    public Artifact resolveArtifact(Artifact inputArtifact) throws MojoExecutionException {
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
