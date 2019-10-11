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
import java.nio.file.Path;
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
    private final Path outputDirectory;
    private boolean ignoreSigningInformation;
    private final List<String> launchers = new ArrayList<>();
    private final Log log;
    private final Integer compression;
    private final boolean stripDebug;
    private final List<String> excludeResourcesPatterns;

    public CreateRuntimeImage(Set<Path> modulePath, List<String> modules, List<String> launcherNames, List<String> launcherModules,
                              Path outputDirectory, Integer compression, boolean stripDebug, boolean ignoreSigningInformation, List<String> excludeResourcesPatterns, Log log) {
        this.modulePath = ( modulePath != null ? modulePath : Collections.emptySet() );
        this.modules = getModules( modules );
        this.outputDirectory = outputDirectory;
        this.ignoreSigningInformation = ignoreSigningInformation;
        
        if (launcherNames != null && launcherModules != null) {
          for (int i=0; i < launcherNames.size() && i < launcherModules.size(); i++) {
            this.launchers.add(launcherNames.get(i)  + "=" + launcherModules.get(i));
          }
        }
        
        this.compression = compression;
        this.stripDebug = stripDebug;
        this.excludeResourcesPatterns = excludeResourcesPatterns;
        this.log = log;
    }

    private static List<String> getModules(List<String> modules) {
        if ( modules == null || modules.isEmpty() ) {
            throw new IllegalArgumentException("At least one module must be added using the <modules> configuration property.");
        }

        return Collections.unmodifiableList( modules );
    }

    public void run() {
        runJlink();
    }

    private void runJlink() throws AssertionError {
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

        for (String launcher : launchers) {
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

        log.debug( "Running jlink: " + String.join( " ", command ) );

        ProcessExecutor.run( "jlink", command, log );
    }
}
