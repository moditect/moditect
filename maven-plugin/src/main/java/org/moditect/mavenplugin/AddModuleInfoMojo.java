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
package org.moditect.mavenplugin;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.artifact.DefaultArtifactCoordinate;
import org.apache.maven.shared.artifact.resolve.ArtifactResolver;
import org.apache.maven.shared.artifact.resolve.ArtifactResolverException;
import org.moditect.commands.AddModuleInfo;

/**
 * @author Gunnar Morling
 */
@Mojo(name = "add-module-info", defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
public class AddModuleInfoMojo extends AbstractMojo {

    @Component
    private ArtifactResolver artifactResolver;

    @Component
    private ArtifactHandlerManager artifactHandlerManager;

    @Parameter( defaultValue = "${session}", readonly = true, required = true )
    private MavenSession session;

    @Parameter(property = "outputDirectory", defaultValue = "${project.build.directory}/modules")
    private File outputDirectory;

    @Parameter
    private List<ModuleConfiguration> modules;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Path outputPath = outputDirectory.toPath();

        if ( !outputDirectory.exists() ) {
            outputDirectory.mkdirs();
        }

        for ( ModuleConfiguration moduleConfiguration : modules ) {
            AddModuleInfo addModuleInfo = new AddModuleInfo(
                moduleConfiguration.getModuleInfoSource(),
                moduleConfiguration.getMainClass(),
                getArtifact( moduleConfiguration.getArtifact() ).getFile().toPath(),
                outputPath
            );

            addModuleInfo.run();
        }
    }

    private Artifact getArtifact(ArtifactConfiguration artifactConfiguration) throws MojoExecutionException {
        ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest(
                session.getProjectBuildingRequest()
        );

        DefaultArtifactCoordinate coordinate = new DefaultArtifactCoordinate();
        coordinate.setGroupId( artifactConfiguration.getGroupId() );
        coordinate.setArtifactId( artifactConfiguration.getArtifactId() );
        coordinate.setVersion( artifactConfiguration.getVersion() );
        coordinate.setClassifier( artifactConfiguration.getClassifier() );
        coordinate.setExtension( getExtension( artifactConfiguration ) );

        try {
            Artifact artifact = artifactResolver.resolveArtifact( buildingRequest, coordinate ).getArtifact();

            if ( artifact == null ) {
                throw new MojoExecutionException( "Couldn't resolve dependency " + artifactConfiguration.toDependencyString() );
            }

            return artifact;
        }
        catch (ArtifactResolverException e) {
            throw new MojoExecutionException( "Couldn't resolve dependency " + artifactConfiguration.toDependencyString(), e );
        }
    }

    private String getExtension(ArtifactConfiguration artifactConfiguration) {
        if ( artifactConfiguration.getType() == null ) {
            return null;
        }

        ArtifactHandler artifactHandler = artifactHandlerManager.getArtifactHandler( artifactConfiguration.getType() );

        if ( artifactHandler != null ) {
            return artifactHandler.getExtension();
        }
        else {
            return artifactConfiguration.getType();
        }
    }
}
