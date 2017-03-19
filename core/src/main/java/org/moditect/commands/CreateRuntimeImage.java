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
package org.moditect.commands;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final String launcher;
    private final Log log;

    public CreateRuntimeImage(Set<Path> modulePath, List<String> modules, String launcherName, String launcherModule, Path outputDirectory, Log log) {
        this.modulePath = modulePath;
        this.modules = modules;
        this.outputDirectory = outputDirectory;
        this.launcher = launcherName + "=" + launcherModule;
        this.log = log;
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
                .collect( Collectors.joining( File.pathSeparator ) ) +
                File.pathSeparator + javaHome + File.separator + "jmods"
        );
        command.add( "--output" );
        command.add( outputDirectory.toString() );

        if ( launcher != null ) {
            command.add( "--launcher" );
            command.add( launcher );
        }
        log.debug( "Running jlink: " + String.join( " ", command ) );

        ProcessBuilder builder = new ProcessBuilder( command );

        Process process;
        List<String> outputLines = new ArrayList<>();
        try {
            process = builder.start();

            BufferedReader in = new BufferedReader( new InputStreamReader( process.getInputStream() ) );
            String line;
            while ( ( line = in.readLine() ) != null ) {
                outputLines.add( line );
                log.debug( line );
            }

            BufferedReader err = new BufferedReader( new InputStreamReader( process.getErrorStream() ) );
            while ( ( line = err.readLine() ) != null ) {
                log.error( line );
            }

            process.waitFor();
        }
        catch (IOException | InterruptedException e) {
            throw new RuntimeException( "Couldn't run jlink", e );
        }

        if ( process.exitValue() != 0 ) {
            for ( String line : outputLines ) {
                log.error( line );
            }

            throw new RuntimeException( "Execution of jlink failed" );
        }
    }
}
