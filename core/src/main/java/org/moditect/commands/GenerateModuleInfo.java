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

import org.moditect.internal.analyzer.ServiceLoaderUseScanner;
import org.moditect.internal.command.ProcessExecutor;
import org.moditect.internal.compiler.ModuleInfoCompiler;
import org.moditect.model.DependencyDescriptor;
import org.moditect.model.PackageNamePattern;
import org.moditect.model.PackageNamePattern.Kind;
import org.moditect.spi.log.Log;

import com.github.javaparser.JavaParser;
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
    private final List<PackageNamePattern> exportPatterns;
    private final List<ModuleRequiresStmt> requiresOverrides;
    private final Path workingDirectory;
    private final Path outputDirectory;
    private final boolean addServiceUses;
    private final Log log;

    public GenerateModuleInfo(Path inputJar, String moduleName, Set<DependencyDescriptor> dependencies, List<PackageNamePattern> exportPatterns, List<String> overrides, Path workingDirectory, Path outputDirectory, boolean addServiceUses, Log log) {
        this.inputJar = inputJar;
        this.moduleName = moduleName;
        this.dependencies = dependencies;
        this.exportPatterns = exportPatterns;
        ModuleDeclaration tempModule = getOverrides( overrides );
        this.requiresOverrides = tempModule.getNodesByType( ModuleRequiresStmt.class );
        this.workingDirectory = workingDirectory;
        this.outputDirectory = outputDirectory;
        this.addServiceUses = addServiceUses;
        this.log = log;
    }

    /**
     * Returns a dummy module with any configured statement overrides, e.g. requires
     * clauses with adjusted modifiers or additional exports.
     */
    private static ModuleDeclaration getOverrides(List<String> overrides) {
        StringBuilder tempModule = new StringBuilder();

        tempModule.append( "module temp {" );

        for (String override : overrides) {
            tempModule.append( override ).append( ";" );
        }

        tempModule.append( "}" );

        return ModuleInfoCompiler.parseModuleInfo( tempModule.toString() );
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

            // update any requires clauses to modules modularized with us to use the assigned module
            // name instead of the automatic module name
            for ( DependencyDescriptor dependency : dependencies ) {
                if ( dependency.getOriginalModuleName().equals( moduleRequiresStmt.getNameAsString() ) && dependency.getAssignedModuleName() != null ) {
                    moduleRequiresStmt.setName( dependency.getAssignedModuleName() );
                }
            }

            for (ModuleRequiresStmt override : requiresOverrides) {
                if ( moduleRequiresStmt.getNameAsString().equals( override.getNameAsString() ) ) {
                    moduleRequiresStmt.getModifiers().clear();
                    moduleRequiresStmt.getModifiers().addAll( override.getModifiers() );
                }
            }
        }

        List<ModuleExportsStmt> exportStatements = moduleDeclaration.getNodesByType( ModuleExportsStmt.class );
        for ( ModuleExportsStmt moduleExportsStmt : exportStatements ) {
            applyExportPatterns( moduleDeclaration, moduleExportsStmt );
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

    private ModuleDeclaration applyExportPatterns(ModuleDeclaration moduleDeclaration, ModuleExportsStmt moduleExportsStmt) {
        for (PackageNamePattern pattern : exportPatterns ) {
            if ( pattern.matches( moduleExportsStmt.getNameAsString() ) ) {
                if ( pattern.getKind() == Kind.INCLUSIVE ) {
                    if ( !pattern.getTargetModules().isEmpty() ) {
                        for (String module : pattern.getTargetModules() ) {
                            moduleExportsStmt.getModuleNames().add( JavaParser.parseName( module ) );
                        }
                    }
                }
                else {
                    moduleDeclaration.remove( moduleExportsStmt );
                }

                break;
            }
        }

        return moduleDeclaration;
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
