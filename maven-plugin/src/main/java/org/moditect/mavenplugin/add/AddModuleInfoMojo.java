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
package org.moditect.mavenplugin.add;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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
import org.moditect.mavenplugin.add.model.ModuleConfiguration;
import org.moditect.mavenplugin.common.model.ArtifactConfiguration;

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

    @Parameter(property = "overwriteExistingFiles", defaultValue = "false")
    private boolean overwriteExistingFiles;

    @Parameter
    private List<ModuleConfiguration> modules;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Path outputPath = outputDirectory.toPath();

        if ( !outputDirectory.exists() ) {
            outputDirectory.mkdirs();
        }

        for ( ModuleConfiguration moduleConfiguration : modules ) {
            Path inputFile = getInputFile( moduleConfiguration );
            String moduleInfoSource = getModuleInfoSource( moduleConfiguration );

            AddModuleInfo addModuleInfo = new AddModuleInfo(
                moduleInfoSource,
                moduleConfiguration.getMainClass(),
                inputFile,
                outputPath,
                overwriteExistingFiles
            );

            addModuleInfo.run();
        }
    }

    private Path getInputFile(ModuleConfiguration moduleConfiguration) throws MojoExecutionException {
        if ( moduleConfiguration.getFile() != null ) {
            if ( moduleConfiguration.getArtifact() != null ) {
                throw new MojoExecutionException( "Only one of 'file' and 'artifact' may be specified, but both are given for"
                        + moduleConfiguration.getArtifact().toDependencyString() );
            }
            else {
                return moduleConfiguration.getFile().toPath();
            }
        }
        else if ( moduleConfiguration.getArtifact() != null ) {
            return getArtifact( moduleConfiguration.getArtifact() ).getFile().toPath();
        }
        else {
            throw new MojoExecutionException( "One of 'file' and 'artifact' must be specified" );
        }
    }

    private String getModuleInfoSource(ModuleConfiguration moduleConfiguration) throws MojoExecutionException {
        String fileForLogging = moduleConfiguration.getFile() != null ? moduleConfiguration.getFile().getPath()
                : moduleConfiguration.getArtifact().toDependencyString();

        if ( moduleConfiguration.getModuleInfoSource() != null ) {
            if ( moduleConfiguration.getModuleInfoFile() != null ) {
                throw new MojoExecutionException( "Only one of 'moduleInfoFile' and 'moduleInfoSource' may be specified, but both are given for "
                        + fileForLogging );
            }
            else {
                return moduleConfiguration.getModuleInfoSource();
            }
        }
        else if ( moduleConfiguration.getModuleInfoFile() != null ) {
            return getLines( moduleConfiguration.getModuleInfoFile().toPath() );
        }
        else {
            throw new MojoExecutionException(
                    "One of 'moduleInfoFile' or 'moduleInfoSource' must be specified for " + fileForLogging );
        }
    }

    private String getLines(Path file) throws MojoExecutionException {
        try {
            return new String(Files.readAllBytes( file ) );
        }
        catch (IOException e) {
            throw new MojoExecutionException( "Couldn't read file " + file );
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
