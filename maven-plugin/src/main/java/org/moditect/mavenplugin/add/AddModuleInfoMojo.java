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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.moditect.commands.AddModuleInfo;
import org.moditect.mavenplugin.add.model.ModuleConfiguration;
import org.moditect.mavenplugin.generate.ModuleInfoGenerator;
import org.moditect.mavenplugin.generate.model.ArtifactIdentifier;
import org.moditect.mavenplugin.util.ArtifactResolutionHelper;
import org.moditect.model.GeneratedModuleInfo;

/**
 * @author Gunnar Morling
 */
@Mojo(name = "add-module-info", defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
public class AddModuleInfoMojo extends AbstractMojo {

    @Component
    private RepositorySystem repoSystem;

    @Parameter( defaultValue = "${repositorySystemSession}", readonly = true, required = true )
    private RepositorySystemSession repoSession;

    @Parameter( defaultValue = "${project.remoteProjectRepositories}", readonly = true, required = true )
    private List<RemoteRepository> remoteRepos;

    @Parameter(readonly = true, defaultValue = "${project.build.directory}/moditect")
    private File workingDirectory;

    @Parameter(property = "outputDirectory", defaultValue = "${project.build.directory}/modules")
    private File outputDirectory;

    @Parameter(property = "overwriteExistingFiles", defaultValue = "false")
    private boolean overwriteExistingFiles;

    @Parameter
    private List<ModuleConfiguration> modules;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Path outputPath = outputDirectory.toPath();

        createDirectories();

        ArtifactResolutionHelper artifactResolutionHelper = new ArtifactResolutionHelper( repoSystem, repoSession, remoteRepos );

        ModuleInfoGenerator moduleInfoGenerator = new ModuleInfoGenerator(
            repoSystem, repoSession, remoteRepos, artifactResolutionHelper, getLog(), workingDirectory, new File(workingDirectory, "generated-sources" )
        );

        Map<ArtifactIdentifier, String> assignedNamesByModule = getAssignedModuleNamesByModule( artifactResolutionHelper );

        for ( ModuleConfiguration moduleConfiguration : modules ) {
            Path inputFile = getInputFile( moduleConfiguration, artifactResolutionHelper );
            String moduleInfoSource = getModuleInfoSource( moduleConfiguration, moduleInfoGenerator, assignedNamesByModule );

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

    private Path getInputFile(ModuleConfiguration moduleConfiguration, ArtifactResolutionHelper artifactResolutionHelper) throws MojoExecutionException {
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
            return artifactResolutionHelper.resolveArtifact( moduleConfiguration.getArtifact() ).getFile().toPath();
        }
        else {
            throw new MojoExecutionException( "One of 'file' and 'artifact' must be specified" );
        }
    }

    private String getModuleInfoSource(ModuleConfiguration moduleConfiguration, ModuleInfoGenerator moduleInfoGenerator, Map<ArtifactIdentifier, String> assignedNamesByModule) throws MojoExecutionException {
        String fileForLogging = moduleConfiguration.getFile() != null ? moduleConfiguration.getFile().getPath()
                : moduleConfiguration.getArtifact().toDependencyString();

        if ( moduleConfiguration.getModuleInfo() != null && moduleConfiguration.getModuleInfoSource() == null && moduleConfiguration.getModuleInfoFile() == null ) {
            GeneratedModuleInfo generatedModuleInfo = moduleInfoGenerator.generateModuleInfo(
                    moduleConfiguration.getArtifact(),
                    Collections.emptyList(),
                    moduleConfiguration.getModuleInfo(),
                    assignedNamesByModule
            );

            return getLines( generatedModuleInfo.getPath() );
        }
        else if ( moduleConfiguration.getModuleInfo() == null && moduleConfiguration.getModuleInfoSource() != null && moduleConfiguration.getModuleInfoFile() == null ) {
            return moduleConfiguration.getModuleInfoSource();
        }
        else if ( moduleConfiguration.getModuleInfo() == null && moduleConfiguration.getModuleInfoSource() == null && moduleConfiguration.getModuleInfoFile() != null ) {
            return getLines( moduleConfiguration.getModuleInfoFile().toPath() );
        }
        else {
            throw new MojoExecutionException( "Either 'moduleInfo' or 'moduleInfoFile' or 'moduleInfoSource' must be specified for " + fileForLogging);
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

    private void createDirectories() {
        if ( !workingDirectory.exists() ) {
            workingDirectory.mkdirs();
        }

        File internalGeneratedSourcesDir = new File(workingDirectory, "generated-sources" );
        if ( !internalGeneratedSourcesDir.exists() ) {
            internalGeneratedSourcesDir.mkdirs();
        }

        if ( !outputDirectory.exists() ) {
            outputDirectory.mkdirs();
        }
    }

    private Map<ArtifactIdentifier, String> getAssignedModuleNamesByModule(ArtifactResolutionHelper artifactResolutionHelper) throws MojoExecutionException {
        Map<ArtifactIdentifier, String> assignedNamesByModule = new HashMap<>();

        for ( ModuleConfiguration configuredModule : modules ) {
            if ( configuredModule.getModuleInfo() != null ) {
                assignedNamesByModule.put(
                        new ArtifactIdentifier( artifactResolutionHelper.resolveArtifact( configuredModule.getArtifact() ) ),
                        configuredModule.getModuleInfo().getName()
                );
            }
            else if ( configuredModule.getModuleInfoFile() != null ) {
                // TODO
            }
            else if ( configuredModule.getModuleInfoSource() != null ) {
                // TODO
            }
        }

        return assignedNamesByModule;
    }
}
