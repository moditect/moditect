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

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

import org.moditect.internal.compiler.ModuleInfoCompiler;

import com.github.javaparser.ast.modules.ModuleDeclaration;

/**
 * Creates a copy of a given JAR file, adding a module-info.class descriptor.
 *
 * @author Gunnar Morling
 */
public class AddModuleInfo {

    private final String moduleInfoSource;
    private final String mainClass;
    private final String version;
    private final Path inputJar;
    private final Path outputDirectory;
    private final boolean overwriteExistingFiles;

    public AddModuleInfo(String moduleInfoSource, String mainClass, String version, Path inputJar, Path outputDirectory, boolean overwriteExistingFiles) {
        this.moduleInfoSource = moduleInfoSource;
        this.mainClass = mainClass;
        this.version = version;
        this.inputJar = inputJar;
        this.outputDirectory = outputDirectory;
        this.overwriteExistingFiles = overwriteExistingFiles;
    }

    public void run() {
        if ( Files.isDirectory( inputJar ) ) {
            throw new IllegalArgumentException( "Input JAR must not be a directory" );
        }

        if ( !Files.exists( outputDirectory ) ) {
            throw new IllegalArgumentException( "Output directory doesn't exist: "  + outputDirectory);
        }

        Path outputJar = outputDirectory.resolve( inputJar.getFileName() );

        if ( Files.exists( outputJar ) && !overwriteExistingFiles ) {
            throw new RuntimeException(
                    "File " + outputJar + " already exists; either set 'overwriteExistingFiles' to true or specify another output directory" );
        }

        try {
            Files.copy(inputJar, outputJar, StandardCopyOption.REPLACE_EXISTING);
        }
        catch(IOException e) {
            throw new RuntimeException( "Couldn't copy JAR file", e );
        }

        ModuleDeclaration module = ModuleInfoCompiler.parseModuleInfo( moduleInfoSource );
        byte[] clazz = ModuleInfoCompiler.compileModuleInfo( module, mainClass, version );

        Map<String, String> env = new HashMap<>();
        env.put( "create", "true" );
        URI uri = URI.create( "jar:" + outputJar.toUri().toString() );

       try (FileSystem zipfs = FileSystems.newFileSystem( uri, env ) ) {
           Files.write( zipfs.getPath( "module-info.class" ), clazz );
        }
       catch(IOException e) {
            throw new RuntimeException( "Couldn't add module-info.class to JAR", e );
        }
    }
}
