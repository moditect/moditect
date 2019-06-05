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
package org.moditect.mavenplugin.image;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;
import org.moditect.commands.CreateRuntimeImage;
import org.moditect.mavenplugin.image.model.Launcher;
import org.moditect.mavenplugin.util.MojoLog;

/**
 * @author Gunnar Morling
 */
@Mojo(name = "create-runtime-image", defaultPhase = LifecyclePhase.PACKAGE)
public class CreateRuntimeImageMojo extends AbstractMojo {

    @Component
    private ToolchainManager toolchainManager;

    @Parameter(defaultValue = "${session}", readonly = true)
    private MavenSession mavenSession;

    @Parameter
    private String baseJdk;

    @Parameter(defaultValue = "[]")
    private List<File> modulePath;

    @Parameter(property = "outputDirectory", defaultValue = "${project.build.directory}/image")
    private File outputDirectory;

    @Parameter(required = true)
    private List<String> modules;

    @Parameter
    private Launcher launcher;

    @Parameter
    private Integer compression;

    @Parameter(defaultValue = "false")
    private boolean stripDebug;
    
    /**
     * Excludes native commands (such as java/java.exe) from the image
     */
    @Parameter(defaultValue = "false")
    private boolean stripNativeCommands;

    /**
     * Specifies a JMOD section to exclude where section-name is man or headers.
     */
    @Parameter
    private String excludeJmodSection;

    @Parameter
    List<String> excludedResources;

    @Parameter(property = "ignoreSigningInformation", defaultValue = "false")
    private boolean ignoreSigningInformation;

//    @Parameter(property = "moditect.artifact")
//    private String artifactOverride;
//
//    @Parameter(property = "moditect.additionalDependencies")
//    private String additionalDependenciesOverride;
//
//    @Parameter(property = "moditect.moduleName")
//    private String moduleNameOverride;
//
//    @Parameter(property = "moditect.exportExcludes")
//    private String exportExcludesOverride;
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Path jmodsDir = getJModsDir();

        Set<Path> effectiveModulePath = this.modulePath.stream()
                .map(File::toPath)
                .collect(Collectors.toSet());

        effectiveModulePath.add(jmodsDir);

        new CreateRuntimeImage(
                effectiveModulePath,
                modules,
                launcher != null ? launcher.getName() : null,
                launcher != null ? launcher.getModule() : null,
                outputDirectory.toPath(),
                compression,
                stripDebug,
                ignoreSigningInformation,
                getExcludeResourcesPatterns(),
                new MojoLog(getLog()),
                stripNativeCommands,
                excludeJmodSection
        )
                .run();
    }

    /**
     * Returns the directory with the jmod files to be used for creating the
     * image. If {@code baseJdk} has been given, the jmod files from the JDK
     * identified that way will be used; otherwise the jmod files from the JDK
     * running the current build will be used.
     */
    private Path getJModsDir() throws MojoExecutionException {
        if (baseJdk != null) {
            List<Toolchain> toolChains = toolchainManager.getToolchains(mavenSession, "jdk", getToolChainRequirements(baseJdk));
            if (toolChains.isEmpty()) {
                throw new MojoExecutionException("Found no tool chain of type 'jdk' and matching requirements '" + baseJdk + "'");
            } else if (toolChains.size() > 1) {
                throw new MojoExecutionException("Found more than one tool chain of type 'jdk' and matching requirements '" + baseJdk + "'");
            } else {
                Toolchain toolchain = toolChains.get(0);

                String javac = toolchain.findTool("javac");

                // #63; when building on Linux / OS X but creating a Windows runtime image
                // the tool lookup must be for javac.exe explicitly (as the toolchain mechanism
                // itself won't append the suffix if not running this build on Windows
                if (javac == null) {
                    javac = toolchain.findTool("javac.exe");
                }
                if (javac == null) {
                    throw new MojoExecutionException("Couldn't locate toolchain directory");
                }
                return new File(javac)
                        .toPath()
                        .getParent()
                        .getParent()
                        .resolve("jmods");
            }
        } else {
            String javaHome = System.getProperty("java.home");
            return new File(javaHome).toPath().resolve("jmods");
        }
    }

    private Map<String, String> getToolChainRequirements(String baseJdk) throws MojoExecutionException {
        Map<String, String> toolChainRequirements = new HashMap<>();
        String[] requirements = baseJdk.split(",");

        for (String requirement : requirements) {
            String[] keyAndValue = requirement.split("=");
            if (keyAndValue.length != 2) {
                throw new MojoExecutionException(
                        "Toolchain requirements must be given in the form 'key1=value1,key2=value2,...'."
                        + "Given value '" + baseJdk + "' doesn't match this pattern.");
            }

            toolChainRequirements.put(keyAndValue[0].trim(), keyAndValue[1].trim());
        }

        return toolChainRequirements;
    }

    private List<String> getExcludeResourcesPatterns() {
        return excludedResources != null ? excludedResources : Collections.emptyList();
    }
}
