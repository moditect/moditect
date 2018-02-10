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
package org.moditect.gradleplugin.task.add;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.moditect.commands.AddModuleInfo;
import org.moditect.generator.ArtifactResolutionHelper;
import org.moditect.generator.ModuleInfoGenerator;
import org.moditect.gradleplugin.internal.GradleArtifactResolver;
import org.moditect.gradleplugin.log.ModitectGradleLog;
import org.moditect.gradleplugin.task.ModitectExtension;
import org.moditect.model.add.AddModuleConfiguration;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Add task for gradle, this task basically recursively modularizes your dependencies.
 *
 * @author Pratik Parikh
 */
public class AddModuleInfoTask extends DefaultTask {
    @TaskAction
    public void add() throws Exception {
        final ModitectExtension moditectExtension = (ModitectExtension) getProject().getExtensions().getByName(
                "moditect");
        final ModitectExtension.AddModuleInfo addModuleInfo = moditectExtension.getModuleInfo();

        // Null checks needs to be done for configuration needed.
        if(addModuleInfo.getVersion() == null){
            throw new IllegalStateException(
                    "version must be specified for project moditect > moduleInfo > version = project.version ");
        }

        getProject().getLogger().info("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        getProject().getLogger().info(addModuleInfo.toString());
        getProject().getLogger().info("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

        final File generatedSourceDirectory =
                new File( addModuleInfo.getWorkingDirectory(), "generated-sources" );
        final List<AddModuleConfiguration> modules =
                addModuleInfo.getModules().stream().map(module -> module.toCoreObject()).collect(Collectors.toList());

        final GradleArtifactResolver artifactResolver = new GradleArtifactResolver(getProject());
        final ArtifactResolutionHelper artifactResolutionHelper = artifactResolver.getArtifactResolutionHelper();
        final ModuleInfoGenerator moduleInfoGenerator = new ModuleInfoGenerator(artifactResolver,
                artifactResolutionHelper,addModuleInfo.getJdepsExtraArgs(),
                new ModitectGradleLog(getProject().getLogger()), addModuleInfo.getWorkingDirectory(),
                addModuleInfo.getOutputDirectory(),modules);
        try {
            moduleInfoGenerator.add(generatedSourceDirectory,addModuleInfo.isOverwriteExistingFiles(),
                    addModuleInfo.getMainModule().toCoreObject(),addModuleInfo.getBuildDirectory(),
                    addModuleInfo.getArtifactId(), addModuleInfo.getVersion());
        } catch (Exception e) {
            throw new Exception("Problem generating module configuration",e);
        }
    }
}
