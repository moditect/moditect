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

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.moditect.commands.model.DependencyDescriptor;
import org.moditect.compiler.ModuleInfoCompiler;

import com.github.javaparser.ast.modules.ModuleDeclaration;

public class GenerateModuleInfo {

    private final Path inputJar;
    private final String moduleName;
    private final List<DependencyDescriptor> dependencies;
    private final Path workingDirectory;
    private final Path outputDirectory;

    public GenerateModuleInfo(Path inputJar, String moduleName, List<DependencyDescriptor> dependencies, Path workingDirectory, Path outputDirectory) {
        this.inputJar = inputJar;
        this.moduleName = moduleName;
        this.dependencies = dependencies;
        this.workingDirectory = workingDirectory;
        this.outputDirectory = outputDirectory;
    }

    public void run() {
        if ( Files.isDirectory( inputJar ) ) {
            throw new IllegalArgumentException( "Input JAR must not be a directory" );
        }

        if ( !Files.exists( workingDirectory ) ) {
            throw new IllegalArgumentException( "Working directory doesn't exist: "  + workingDirectory );
        }

        if ( !Files.exists( outputDirectory ) ) {
            throw new IllegalArgumentException( "Output directory doesn't exist: "  + outputDirectory );
        }

        Path stagingDir = recreateDirectory( workingDirectory, "staging" );

        runJdeps( stagingDir );

        ModuleDeclaration moduleDeclaration = parseGeneratedModuleInfo( stagingDir );

        if ( moduleName != null ) {
            moduleDeclaration.setName( moduleName );
        }

        writeModuleInfo( moduleDeclaration );
    }

    private void runJdeps(Path stagingDir) throws AssertionError {
        String javaHome = System.getProperty("java.home");
        String jdepsBin = javaHome +
                File.separator + "bin" +
                File.separator + "jdeps";

        List<String> command = new ArrayList<>();
        command.add( jdepsBin );

        command.add( "--generate-module-info" );
        command.add( stagingDir.toString() );

        if ( !dependencies.isEmpty() ) {
            String modules = ModuleFinder.of(
                 dependencies.stream()
                     .map( DependencyDescriptor::getPath )
                     .toArray( Path[]::new )
                 )
                .findAll()
                .stream()
                .map( ModuleReference::descriptor )
                .map( ModuleDescriptor::name )
                .collect( Collectors.joining( "," ) );

            String modulePath = dependencies.stream()
                .map( DependencyDescriptor::getPath )
                .map( Path::toString )
                .collect( Collectors.joining( File.pathSeparator ) );

            command.add( "--add-modules" );
            command.add( modules );
            command.add( "--module-path" );
            command.add( modulePath );
        }

        command.add( inputJar.toString() );

        ProcessBuilder builder = new ProcessBuilder( command )
            .redirectOutput( Redirect.INHERIT )
            .redirectError( Redirect.INHERIT );

        Process process;
        try {
            process = builder.start();
            process.waitFor();
        }
        catch (IOException | InterruptedException e) {
            throw new RuntimeException( "Couldn't run jdeps", e );
        }

        if ( process.exitValue() != 0 ) {
            throw new RuntimeException( "Execution of jdeps failed" );
        }
    }

    private ModuleDeclaration parseGeneratedModuleInfo(Path stagingDir) {
        String generatedModuleName = getGeneratedModuleName( stagingDir );

        Path moduleDir = stagingDir.resolve( generatedModuleName );
        Path moduleInfo = moduleDir.resolve( "module-info.java" );

        return ModuleInfoCompiler.parseModuleInfo( moduleInfo );
    }

    private String getGeneratedModuleName(Path stagingDir) {
        try {
            return Files.find( stagingDir, 2, (p, a) -> p.getFileName().endsWith( "module-info.java" ) )
                    .findFirst()
                    .get()
                    .getParent()
                    .getFileName()
                    .toString();
        }
        catch(IOException e) {
            throw new RuntimeException( e );
        }
    }

    private void writeModuleInfo(ModuleDeclaration moduleDeclaration) {
        Path outputModuleInfo = recreateDirectory( outputDirectory, moduleDeclaration.getNameAsString() )
                .resolve( "module-info.java" );

        try {
            Files.write( outputModuleInfo, moduleDeclaration.toString().getBytes() );
        }
        catch (IOException e) {
            throw new RuntimeException( "Couldn't write module-info.java", e );
        }
    }

    private Path recreateDirectory(Path parent, String directoryName) {
        Path dir = parent.resolve( directoryName );
        try {
            Files.deleteIfExists( dir );
            Files.createDirectory( dir );
        }
        catch (IOException e) {
            throw new RuntimeException( "Couldn't recreate directory " + dir, e );
        }

        return dir;
    }
}
