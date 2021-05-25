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
package org.moditect.commands;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.moditect.internal.command.ProcessExecutor;
import org.moditect.model.JarInclusionPolicy;
import org.moditect.spi.log.Log;

/**
 * Creates a modular runtime image for the given modules and module path, via jlink.
 *
 * @author Gunnar Morling
 */
public class CreateRuntimeImage {

    private static final String DEPENDENCIES_DIRECTORY = "jars";

    private final Set<Path> modulePath;
    private final List<String> modules;
    private final JarInclusionPolicy jarInclusionPolicy;
    private final Set<Path> dependencies;
    private final Path projectJar;
    private final Path outputDirectory;
    private boolean ignoreSigningInformation;
    private final String launcher;
    private final Log log;
    private final Integer compression;
    private final boolean stripDebug;
    private final boolean noHeaderFiles;
    private final boolean noManPages;
    private final List<String> excludeResourcesPatterns;
    private final boolean bindServices;

    public CreateRuntimeImage(Set<Path> modulePath, List<String> modules, JarInclusionPolicy jarInclusionPolicy,
                              Set<Path> dependencies, Path projectJar, String launcherName, String launcherModule,
                              Path outputDirectory, Integer compression, boolean stripDebug,
                              boolean ignoreSigningInformation, List<String> excludeResourcesPatterns, Log log,
                              boolean noHeaderFiles, boolean noManPages, boolean bindServices) {
        this.modulePath = ( modulePath != null ? modulePath : Collections.emptySet() );
        this.modules = getModules( modules );
        this.jarInclusionPolicy = jarInclusionPolicy;
        this.dependencies = dependencies;
        this.projectJar = projectJar;
        this.outputDirectory = outputDirectory;
        this.ignoreSigningInformation = ignoreSigningInformation;
        this.launcher = launcherName != null && launcherModule != null ? launcherName + "=" + launcherModule : null;
        this.compression = compression;
        this.stripDebug = stripDebug;
        this.excludeResourcesPatterns = excludeResourcesPatterns;
        this.log = log;
        this.noHeaderFiles = noHeaderFiles;
        this.noManPages = noManPages;
        this.bindServices = bindServices;
    }

    private static List<String> getModules(List<String> modules) {
        if ( modules == null || modules.isEmpty() ) {
            throw new IllegalArgumentException("At least one module must be added using the <modules> configuration property.");
        }

        return Collections.unmodifiableList( modules );
    }

    public void run() throws IOException {
        deleteImageFolder();
        runJlink();
        log.info("Done creating image");
        copyJars();
    }

    private void deleteImageFolder() throws IOException {
        if (!Files.exists(outputDirectory)) {
            return;
        }

        log.info("Deleting image directory " + outputDirectory);

        Files.walkFileTree(outputDirectory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null)
                    throw exc;
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void copyJars() throws IOException {
        Path jarDirectory = outputDirectory.resolve(DEPENDENCIES_DIRECTORY);
        Files.createDirectories(jarDirectory);

        if (jarInclusionPolicy.includeAppJar()) {
            copyAppJar(jarDirectory);
        }
        if (jarInclusionPolicy.includeDependencies()) {
            copyDependencyJars(jarDirectory);
        }
    }

    private void copyAppJar(Path jarDirectory) throws IOException {
        log.info("Copying project JAR");
        Path target = jarDirectory.resolve(projectJar.getFileName());
        Files.copy(projectJar, target);
        log.debug(String.format("Done copying app JAR %s to %s", projectJar, target));
    }

    private void copyDependencyJars(Path jarDirectory) throws IOException {
        log.info("Copying project dependencies");

        for (Path dependency : dependencies) {
            Path target = jarDirectory.resolve(dependency.getFileName());
            Files.copy(dependency, target);
            log.debug(String.format("Done copying dependency %s to %s", dependency, target));
        }

        log.info("Done copying project dependencies");
    }

    private void runJlink() throws AssertionError {
        log.info("Running jlink");
        String javaHome = System.getProperty("java.home");
        String jlinkBin = javaHome +
                File.separator + "bin" +
                File.separator + "jlink";

        List<String> command = new ArrayList<>();
        command.add( jlinkBin );

        command.add( "--add-modules" );
        command.add( String.join( ",", modules ) );
        command.add( "--module-path" );
        command.add( modulePath.stream()
                .map( Path::toString )
                .collect( Collectors.joining( File.pathSeparator ) )
        );
        command.add( "--output" );
        command.add( outputDirectory.toString() );

        if ( launcher != null ) {
            command.add( "--launcher" );
            command.add( launcher );
        }

        if ( compression != null ) {
            command.add( "--compress" );
            command.add( compression.toString() );
        }

        if ( stripDebug ) {
            command.add( "--strip-debug" );
        }

        if (ignoreSigningInformation) {
            command.add( "--ignore-signing-information" );
        }

        if ( !excludeResourcesPatterns.isEmpty() ) {
            command.add( "--exclude-resources=" + String.join( ",", excludeResourcesPatterns ) );
        }

        if ( noHeaderFiles ) {
            command.add( "--no-header-files" );
        }

        if ( noManPages ) {
            command.add( "--no-man-pages" );
        }

        if ( bindServices ) {
            command.add( "--bind-services" );
        }

        log.debug( "Running jlink: " + String.join( " ", command ) );

        ProcessExecutor.run( "jlink", command, log );
    }
}
