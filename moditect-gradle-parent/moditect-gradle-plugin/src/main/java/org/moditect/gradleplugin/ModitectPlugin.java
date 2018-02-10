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
package org.moditect.gradleplugin;

import org.gradle.api.*;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.moditect.gradleplugin.task.ModitectExtension;
import org.moditect.gradleplugin.task.add.AddModuleInfoTask;
import org.moditect.gradleplugin.task.generate.GenerateModuleInfoTask;
import org.moditect.gradleplugin.task.image.CreateRuntimeImageTask;

import java.util.List;

/**
 * Plugin to wire the add, generate and image tasks.
 *
 * @author Pratik Parikh
 */
public class ModitectPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getLogger().debug("Welcome to moditect plugin");

        project.getPlugins().withType(JavaPlugin.class, new Action<JavaPlugin>() {
            public void execute(JavaPlugin javaPlugin) {
                final JavaPluginConvention javaConvention =
                        project.getConvention().getPlugin(JavaPluginConvention.class);
                final JavaVersion javaTargetVersion = javaConvention.getTargetCompatibility();
                final JavaVersion javaSourceVersion = javaConvention.getSourceCompatibility();

                if(javaSourceVersion != null && javaSourceVersion.compareTo(javaTargetVersion) >= 0){
                    project.getLogger().debug("java source and target are the same version "
                            .concat(javaTargetVersion.getMajorVersion()));
                }

                if(javaTargetVersion.equals(JavaVersion.VERSION_1_5)
                        || javaTargetVersion.equals(JavaVersion.VERSION_1_6)
                        || javaTargetVersion.equals(JavaVersion.VERSION_1_7)
                        || javaTargetVersion.equals(JavaVersion.VERSION_1_8)){
                    throw new IllegalStateException(
                            "Target for java should be set to Java 9 or higher, current version is : ".concat(javaTargetVersion.toString()));
                }

                project.getLogger().debug("Targeted Java is of correct version.");

            }
        });

        project.getExtensions().create("moditect",ModitectExtension.class,project);

        final Task jarTask = project.getTasks().findByName("jar");

        final Task generateModuleInfo = project.getTasks().create("generateModuleInfo", GenerateModuleInfoTask.class);
        generateModuleInfo.setDependsOn(List.of(jarTask));
        generateModuleInfo.setMustRunAfter(List.of(jarTask));
        jarTask.setFinalizedBy(List.of(generateModuleInfo));

        final Task addModuleInfo = project.getTasks().create("addModuleInfo", AddModuleInfoTask.class);
        jarTask.setDependsOn(List.of(addModuleInfo));
        jarTask.setMustRunAfter(List.of(addModuleInfo));
        addModuleInfo.setFinalizedBy(List.of(jarTask));

        final Task createRuntimeImage = project.getTasks().create("createRuntimeImage", CreateRuntimeImageTask.class);
        createRuntimeImage.dependsOn(List.of(generateModuleInfo));
        createRuntimeImage.setMustRunAfter(List.of(generateModuleInfo));
    }
}
