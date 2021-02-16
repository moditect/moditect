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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.moditect.internal.command.ProcessExecutor;
import org.moditect.spi.log.Log;

/**
 * Creates a modular runtime image for the given modules and module path, via jlink.
 *
 * @author Gunnar Morling
 */
public class CreateRuntimeImage {

    private final Set<Path> modulePath;
    private final List<String> modules;
    private final boolean includeJars;
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

    public CreateRuntimeImage(Set<Path> modulePath, List<String> modules, boolean includeJars, Path projectJar,
                              String launcherName, String launcherModule,
                              Path outputDirectory, Integer compression, boolean stripDebug,
                              boolean ignoreSigningInformation, List<String> excludeResourcesPatterns, Log log,
                              boolean noHeaderFiles, boolean noManPages, boolean bindServices) {
        this.modulePath = ( modulePath != null ? modulePath : Collections.emptySet() );
        this.modules = getModules( modules );
        this.includeJars = includeJars;
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
        runJlink();
        if (includeJars)
            copyJars();
        log.info("Done creating image");
    }

    private void copyJars() throws IOException {
        log.info("Copying project JAR");
        Path jarDirectory = outputDirectory.resolve("jars");
        Files.createDirectories(jarDirectory);
        Files.copy(projectJar, jarDirectory.resolve(projectJar.getFileName()));
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
