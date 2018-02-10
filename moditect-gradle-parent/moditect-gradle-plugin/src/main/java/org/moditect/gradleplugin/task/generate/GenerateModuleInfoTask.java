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
package org.moditect.gradleplugin.task.generate;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.moditect.commands.AddModuleInfo;
import org.moditect.commands.GenerateModuleInfo;
import org.moditect.generator.ArtifactResolutionHelper;
import org.moditect.generator.ModuleInfoGenerator;
import org.moditect.gradleplugin.internal.GradleArtifactResolver;
import org.moditect.gradleplugin.log.ModitectGradleLog;
import org.moditect.gradleplugin.task.ModitectExtension;
import org.moditect.model.add.AddModuleConfiguration;
import org.moditect.model.generate.ModuleConfiguration;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Generates a module-info.java task for a dependency using jdeps tool.
 *
 * @author Pratik Parikh
 */
public class GenerateModuleInfoTask extends DefaultTask {

    @TaskAction
    public void generate() throws Exception {
        final ModitectExtension moditectExtension = (ModitectExtension) getProject().getExtensions().getByName(
                "moditect");
        final ModitectExtension.GenerateModuleInfo generateModuleInfo = moditectExtension.getGenerateModuleInfo();
        final AddModuleInfo addModuleInfo = (AddModuleInfo)getExtensions().findByName("finalizeAddModuleInfo");
        if(generateModuleInfo == null) {
            final GradleArtifactResolver artifactResolver = new GradleArtifactResolver(getProject());
            final ArtifactResolutionHelper artifactResolutionHelper = artifactResolver.getArtifactResolutionHelper();

            final List<ModuleConfiguration> modules =
                    generateModuleInfo.getModules().stream().map(module -> module.toCoreObject()).collect(Collectors.toList());
            final ModuleInfoGenerator moduleInfoGenerator = new ModuleInfoGenerator(artifactResolver,
                    artifactResolutionHelper, generateModuleInfo.getJdepsExtraArgs(),
                    new ModitectGradleLog(getProject().getLogger()), generateModuleInfo.getWorkingDirectory(),
                    generateModuleInfo.getOutputDirectory(), modules, generateModuleInfo.getArtifact(),
                    generateModuleInfo.getModuleName(), generateModuleInfo.getAdditionalDependencies(),
                    generateModuleInfo.getExportExcludes(), generateModuleInfo.getAddServiceUses());
            try {
                moduleInfoGenerator.generate();
            } catch (Exception e) {
                throw new Exception("Problem generating module configuration", e);
            }
        } else if (addModuleInfo != null){
            addModuleInfo.initalizeModuleInfoSource().run();
        }
    }

}
