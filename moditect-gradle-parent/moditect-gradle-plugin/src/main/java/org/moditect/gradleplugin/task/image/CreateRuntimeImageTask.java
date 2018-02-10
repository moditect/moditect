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
package org.moditect.gradleplugin.task.image;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.moditect.commands.CreateRuntimeImage;
import org.moditect.gradleplugin.log.ModitectGradleLog;
import org.moditect.gradleplugin.task.ModitectExtension;
import org.moditect.model.image.Launcher;
import org.moditect.spi.log.Log;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Create your runtime image using jlink tool from the pre-modularized dependencies using add, jar and generate tasks.
 *
 * @author Pratik Parikh
 */
public class CreateRuntimeImageTask extends DefaultTask {

    public CreateRuntimeImageTask(){
    }

    @TaskAction
    public void build(){
        final String javaHome = System.getProperty("java.home");
        final Path jmodsDirectory = new File( javaHome ).toPath().resolve( "jmods" );
        final ModitectExtension moditectExtension = (ModitectExtension) getProject().getExtensions().getByName(
                "moditect");
        final ModitectExtension.ApplicationImage image = moditectExtension.getImage();
        if (image.getModulePath() == null || image.getModulePath().isEmpty() ) {
            throw new IllegalArgumentException(
                    "At least one module path must be added using the modulePath:['$buildDir/modules'] configuration property.");
        }
        if (image.getModules() == null || image.getModules().isEmpty() ) {
            throw new IllegalArgumentException(
                    "At least one module path must be added using the modules:['com.example'] configuration property.");
        }
        if ((image.getLauncher() == null || image.getLauncher().getName() == null)
                || (image.getLauncher() == null
                || image.getLauncher().getModule() == null)) {
            throw new IllegalArgumentException("Launcher and Launcher module name must be configuration property.");
        }
        if(image.getCompression() < -1){
            throw new IllegalArgumentException("Compression must be configuration property, and should be valid.");
        }
        image.getModulePath().add(jmodsDirectory);
        final CreateRuntimeImage createRuntimeImage = new CreateRuntimeImage(image.getModulePath()
                ,image.getModules(),image.getLauncher().getName(),
                image.getLauncher().getModule(),image.getOutputDirectory(),
                image.getCompression(),image.getStripDebug(),
                image.getExcludeResources() == null ? Collections.emptyList() :
                        image.getExcludeResources(),
                new ModitectGradleLog(getProject().getLogger()));
        createRuntimeImage.run();
    }
}
