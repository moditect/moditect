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
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.moditect.internal.analyzer.ServiceLoaderUseScanner;
import org.moditect.internal.command.ProcessExecutor;
import org.moditect.internal.compiler.ModuleInfoCompiler;
import org.moditect.model.DependencyDescriptor;
import org.moditect.spi.log.Log;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.modules.ModuleDeclaration;
import com.github.javaparser.ast.modules.ModuleExportsStmt;
import com.github.javaparser.ast.modules.ModuleRequiresStmt;
import com.github.javaparser.ast.modules.ModuleUsesStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

public class GenerateModuleInfo {

    private final Path inputJar;
    private final String moduleName;
    private final Set<DependencyDescriptor> dependencies;
    private List<Pattern> exportExcludes;
    private final Path workingDirectory;
    private final Path outputDirectory;
    private final boolean addServiceUses;
    private final Log log;

    public GenerateModuleInfo(Path inputJar, String moduleName, Set<DependencyDescriptor> dependencies, List<Pattern> exportExcludes, Path workingDirectory, Path outputDirectory, boolean addServiceUses, Log log) {
        this.inputJar = inputJar;
        this.moduleName = moduleName;
        this.dependencies = dependencies;
        this.exportExcludes = exportExcludes;
        this.workingDirectory = workingDirectory;
        this.outputDirectory = outputDirectory;
        this.addServiceUses = addServiceUses;
        this.log = log;
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

        Map<String, Boolean> optionalityPerModule = runJdeps();
        ModuleDeclaration moduleDeclaration = parseGeneratedModuleInfo();
        updateModuleInfo( optionalityPerModule, moduleDeclaration );

        writeModuleInfo( moduleDeclaration );
    }

    private void updateModuleInfo(Map<String, Boolean> optionalityPerModule, ModuleDeclaration moduleDeclaration) {
        List<ModuleRequiresStmt> requiresStatements = moduleDeclaration.getNodesByType( ModuleRequiresStmt.class );
        for ( ModuleRequiresStmt moduleRequiresStmt : requiresStatements ) {
            if ( Boolean.TRUE.equals( optionalityPerModule.get( moduleRequiresStmt.getNameAsString() ) ) ) {
                moduleRequiresStmt.addModifier( Modifier.STATIC );
            }
        }

        List<ModuleExportsStmt> exportStatements = moduleDeclaration.getNodesByType( ModuleExportsStmt.class );
        for ( ModuleExportsStmt moduleExportsStmt : exportStatements ) {
            if ( isExcluded( moduleExportsStmt ) ) {
                moduleDeclaration.remove( moduleExportsStmt );
            }
        }

        if ( moduleName != null ) {
            moduleDeclaration.setName( moduleName );
        }

        if ( addServiceUses ) {
            Set<String> usedServices = ServiceLoaderUseScanner.getUsedServices( inputJar );
            for ( String usedService : usedServices ) {
                moduleDeclaration.getModuleStmts().add( new ModuleUsesStmt( getType( usedService ) ) );
            }
        }
    }

    private ClassOrInterfaceType getType(String fqn) {
        String[] parts = fqn.split( "\\." );

        ClassOrInterfaceType scope = null;
        String name = null;

        if ( parts.length == 1 ) {
            scope = null;
            name = parts[0];
        }
        else {
            ClassOrInterfaceType parentScope = null;
            for( int i = 0; i < parts.length - 1; i++ ) {
                scope = new ClassOrInterfaceType( parentScope, parts[i] );
                parentScope = scope;
            }
            name = parts[parts.length - 1];
        }

        return new ClassOrInterfaceType( scope, name );
    }

    private boolean isExcluded(ModuleExportsStmt moduleExportsStmt) {
        return exportExcludes.stream()
            .anyMatch( exclude -> exclude.matcher( moduleExportsStmt.getNameAsString() ).matches() );
    }

    private Map<String, Boolean> runJdeps() throws AssertionError {
        Map<String, Boolean> optionalityPerModule = new HashMap<>();

        String javaHome = System.getProperty( "java.home" );
        String jdepsBin = javaHome +
                File.separator + "bin" +
                File.separator + "jdeps";

        List<String> command = new ArrayList<>();
        command.add( jdepsBin );

        command.add( "--generate-module-info" );
        command.add( workingDirectory.toString() );

        if ( !dependencies.isEmpty() ) {
            StringBuilder modules = new StringBuilder();
            StringBuilder modulePath = new StringBuilder();
            boolean isFirst = true;

            for ( DependencyDescriptor dependency : dependencies ) {
                if ( isFirst ) {
                    isFirst = false;
                }
                else {
                    modules.append( "," );
                    modulePath.append( File.pathSeparator );
                }
                ModuleDescriptor descriptor = ModuleFinder.of( dependency.getPath() )
                        .findAll()
                        .iterator()
                        .next()
                        .descriptor();

                modules.append( descriptor.name() );
                optionalityPerModule.put( descriptor.name(), dependency.isOptional() );
                modulePath.append( dependency.getPath() );
            }

            command.add( "--add-modules" );
            command.add( modules.toString() );
            command.add( "--module-path" );
            command.add( modulePath.toString() );
        }

        command.add( inputJar.toString() );

        log.debug( "Running jdeps: " + String.join( " ", command ) );

        ProcessExecutor.run( "jdeps", command, log );

        return optionalityPerModule;
    }

    private ModuleDeclaration parseGeneratedModuleInfo() {
        String generatedModuleName = ModuleFinder.of( inputJar )
                .findAll()
                .iterator()
                .next()
                .descriptor()
                .name();

        Path moduleDir = workingDirectory.resolve( generatedModuleName );
        Path moduleInfo = moduleDir.resolve( "module-info.java" );

        return ModuleInfoCompiler.parseModuleInfo( moduleInfo );
    }

    private void writeModuleInfo(ModuleDeclaration moduleDeclaration) {
        Path outputModuleInfo = recreateDirectory( outputDirectory, moduleDeclaration.getNameAsString() )
                .resolve( "module-info.java" );

        try {
            Files.write( outputModuleInfo, moduleDeclaration.toString().getBytes() );

            log.info( "Created module descriptor at " + outputModuleInfo );
        }
        catch (IOException e) {
            throw new RuntimeException( "Couldn't write module-info.java", e );
        }
    }

    private Path recreateDirectory(Path parent, String directoryName) {
        Path dir = parent.resolve( directoryName );

        try {
            if ( Files.exists( dir ) ) {
                Files.walkFileTree( dir, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete( file );
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.delete( dir );
                        return FileVisitResult.CONTINUE;
                    }
                });
            }

            Files.createDirectory( dir );
        }
        catch (IOException e) {
            throw new RuntimeException( "Couldn't recreate directory " + dir, e );
        }

        return dir;
    }
}
