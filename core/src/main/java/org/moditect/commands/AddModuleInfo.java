/*
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

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.moditect.internal.compiler.ModuleInfoCompiler;

import com.github.javaparser.ast.modules.ModuleDeclaration;

/**
 * Creates a copy of a given JAR file, adding a module-info.class descriptor.
 *
 * @author Gunnar Morling
 */
public class AddModuleInfo {

    private static final String NO_JVM_VERSION = "base";

    private final String moduleInfoSource;
    private final String mainClass;
    private final String version;
    private final Path inputJar;
    private final Path outputDirectory;
    private final Integer jvmVersion;
    private final boolean overwriteExistingFiles;

    public AddModuleInfo(String moduleInfoSource, String mainClass, String version, Path inputJar, Path outputDirectory, String jvmVersion, boolean overwriteExistingFiles) {
        this.moduleInfoSource = moduleInfoSource;
        this.mainClass = mainClass;
        this.version = version;
        this.inputJar = inputJar;
        this.outputDirectory = outputDirectory;

        // #67 It'd be nice to use META-INF/services/9 by default to avoid conflicts with legacy
        // classpath scanners, but this causes isses with subsequent jdeps invocations if there
        // are MR-JARs and non-MR JARs passed to it due to https://bugs.openjdk.java.net/browse/JDK-8207162
        if (jvmVersion == null || jvmVersion.equals(NO_JVM_VERSION)) {
            this.jvmVersion = null;
        }
        else {
            try {
                this.jvmVersion = Integer.valueOf(jvmVersion);
                if (this.jvmVersion < 9) {
                    throw new NumberFormatException();
                }
            }
            catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid JVM Version: " + jvmVersion + ". Allowed values are 'base' and integer values >= 9." );
            }
        }
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
           if (jvmVersion == null) {
               Files.write( zipfs.getPath( "module-info.class" ), clazz,
                 StandardOpenOption.CREATE,
                 StandardOpenOption.WRITE,
                 StandardOpenOption.TRUNCATE_EXISTING );
           }
           else {
               Path path = zipfs.getPath( "META-INF/versions", jvmVersion.toString(), "module-info.class" );
               Files.createDirectories( path.getParent() );
               Files.write( path, clazz,
                 StandardOpenOption.CREATE,
                 StandardOpenOption.WRITE,
                 StandardOpenOption.TRUNCATE_EXISTING );

               Path manifestPath = zipfs.getPath( "META-INF/MANIFEST.MF" );
               Manifest manifest;
               if ( Files.exists( manifestPath ) ) {
                   manifest = new Manifest( Files.newInputStream( manifestPath ) );
               }
               else {
                   manifest = new Manifest();
                   manifest.getMainAttributes().put( Attributes.Name.MANIFEST_VERSION, "1.0" );
               }

               manifest.getMainAttributes().put( new Attributes.Name("Multi-Release"), "true" );
               try (OutputStream manifestOs = Files.newOutputStream( manifestPath, StandardOpenOption.TRUNCATE_EXISTING )) {
                   manifest.write( manifestOs );
               }
           }
       }
       catch(IOException e) {
            throw new RuntimeException( "Couldn't add module-info.class to JAR", e );
        }
    }
}
