/**
 *  Copyright 2017 The ModiTect authors
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.module.FindException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.spi.ToolProvider;

import org.moditect.internal.analyzer.ServiceLoaderUseScanner;
import org.moditect.internal.compiler.ModuleInfoCompiler;
import org.moditect.model.DependencePattern;
import org.moditect.model.DependencyDescriptor;
import org.moditect.model.GeneratedModuleInfo;
import org.moditect.model.PackageNamePattern;
import org.moditect.model.PackageNamePattern.Kind;
import org.moditect.spi.log.Log;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.modules.ModuleDeclaration;
import com.github.javaparser.ast.modules.ModuleExportsStmt;
import com.github.javaparser.ast.modules.ModuleOpensStmt;
import com.github.javaparser.ast.modules.ModuleRequiresStmt;
import com.github.javaparser.ast.modules.ModuleUsesStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

public class GenerateModuleInfo {

    private final Path inputJar;
    private final String autoModuleNameForInputJar;
    private final String moduleName;
    private final boolean open;
    private final Set<DependencyDescriptor> dependencies;
    private final List<PackageNamePattern> exportPatterns;
    private final List<PackageNamePattern> opensPatterns;
    private final List<DependencePattern> requiresPatterns;
    private final Set<String> uses;
    private final Path workingDirectory;
    private final Path outputDirectory;
    private final boolean addServiceUses;
    private final ServiceLoaderUseScanner serviceLoaderUseScanner;
    private final List<String> jdepsExtraArgs;
    private final Log log;
    private ToolProvider jdeps;

    public GenerateModuleInfo(Path inputJar, String moduleName, boolean open, Set<DependencyDescriptor> dependencies, List<PackageNamePattern> exportPatterns, List<PackageNamePattern> opensPatterns, List<DependencePattern> requiresPatterns, Path workingDirectory, Path outputDirectory, Set<String> uses, boolean addServiceUses, List<String> jdepsExtraArgs, Log log) {
        String autoModuleNameForInputJar = getAutoModuleNameFromInputJar( inputJar );

        // if no valid auto module name can be derived for the input JAR, create a copy of it and
        // inject the target module name into the manifest ("Automatic-Module-Name"), as otherwise
        // jdeps will fail (issue #37)
        if ( autoModuleNameForInputJar != null ) {
            this.autoModuleNameForInputJar = autoModuleNameForInputJar;
            this.inputJar = inputJar;
        }
        else {
            this.autoModuleNameForInputJar = moduleName;
            this.inputJar = createCopyWithAutoModuleNameManifestHeader( workingDirectory, inputJar, moduleName );
        }

        this.moduleName = moduleName;
        this.open = open;
        this.dependencies = dependencies;
        this.exportPatterns = exportPatterns;
        this.opensPatterns = opensPatterns;
        this.requiresPatterns = requiresPatterns;
        this.workingDirectory = workingDirectory;
        this.outputDirectory = outputDirectory;
        this.uses = uses;
        this.addServiceUses = addServiceUses;
        this.serviceLoaderUseScanner = new ServiceLoaderUseScanner( log );
        this.jdepsExtraArgs = jdepsExtraArgs != null ? jdepsExtraArgs : Collections.emptyList();
        this.log = log;

        Optional<ToolProvider> jdeps = ToolProvider.findFirst( "jdeps" );

        if ( jdeps.isPresent() ) {
            this.jdeps = jdeps.get();
        }
        else {
            throw new RuntimeException( "jdeps tool not found" );
        }
    }

    private static String getAutoModuleNameFromInputJar(Path inputJar) {
        try {
            return ModuleFinder.of( inputJar )
                    .findAll()
                    .iterator()
                    .next()
                    .descriptor()
                    .name();
        }
        catch (FindException e) {
            if ( e.getCause() != null && e.getCause().getMessage().contains( "Invalid module name" ) ) {
                return null;
            }

            throw e;
        }
    }

    private static Path createCopyWithAutoModuleNameManifestHeader(Path workingDirectory, Path inputJar, String moduleName) {
        if ( moduleName == null ) {
            throw new IllegalArgumentException( "No automatic name can be derived for the JAR " + inputJar + ", hence an explicit module name is required" );
        }

        Path copiedJar = createCopy( workingDirectory, inputJar );

        Map<String, String> env = new HashMap<>();
        env.put( "create", "true" );
        URI uri = URI.create( "jar:" + copiedJar.toUri().toString() );

        try ( FileSystem zipfs = FileSystems.newFileSystem( uri, env );
                ByteArrayOutputStream baos = new ByteArrayOutputStream() ) {

            Manifest manifest = getManifest( inputJar );
            manifest.getMainAttributes().putValue( "Automatic-Module-Name", moduleName );

            manifest.write( baos );
            Files.write( zipfs.getPath( "META-INF", "MANIFEST.MF" ), baos.toByteArray() );

            return copiedJar;
        }
        catch(IOException ioe) {
            throw new RuntimeException( "Couldn't inject automatic module name into manifest", ioe );
        }
    }

    private static Path createCopy(Path workingDirectory, Path inputJar) {
        try {
            Path tempDir = Files.createTempDirectory( workingDirectory, null );
            Path copiedJar = tempDir.resolve( inputJar.getFileName() );
            Files.copy( inputJar, copiedJar );

            return copiedJar;
        }
        catch (IOException ieo) {
            throw new RuntimeException( ieo );
        }
    }

    private static Manifest getManifest(Path inputJar) throws IOException {
        try ( JarInputStream jar = new JarInputStream( Files.newInputStream( inputJar ) ) ) {
            Manifest manifest = jar.getManifest();

            return manifest != null ? manifest : new Manifest();
        }
    }

    public GeneratedModuleInfo run() {
        if ( Files.isDirectory( inputJar ) ) {
            throw new IllegalArgumentException( "Input JAR must not be a directory" );
        }

        if ( !Files.exists( workingDirectory ) ) {
            throw new IllegalArgumentException( "Working directory doesn't exist: "  + workingDirectory );
        }

        if ( !Files.exists( outputDirectory ) ) {
            throw new IllegalArgumentException( "Output directory doesn't exist: "  + outputDirectory );
        }

        Map<String, Boolean> optionalityPerModule = generateModuleInfo();
        ModuleDeclaration moduleDeclaration = parseGeneratedModuleInfo();
        updateModuleInfo( optionalityPerModule, moduleDeclaration );

        return writeModuleInfo( moduleDeclaration );
    }

    private void updateModuleInfo(Map<String, Boolean> optionalityPerModule, ModuleDeclaration moduleDeclaration) {
        if ( open ) {
            moduleDeclaration.setOpen( true );
        }

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

            for ( DependencePattern dependence : requiresPatterns ) {
                if ( dependence.matches( moduleRequiresStmt.getNameAsString() ) ) {
                    if ( dependence.isMatchAll() && dependence.getModifiers().isEmpty() ) {
                        moduleRequiresStmt.getModifiers().remove( Modifier.TRANSITIVE );
                    }
                    else {
                        moduleRequiresStmt.getModifiers().clear();
                        dependence.getModifiers()
                            .stream()
                            .map( m -> Modifier.valueOf( m.toUpperCase( Locale.ENGLISH ) ) )
                            .forEach( m -> moduleRequiresStmt.getModifiers().add( m ) );
                    }

                    break;
                }
            }
        }

        List<ModuleExportsStmt> exportStatements = moduleDeclaration.getNodesByType( ModuleExportsStmt.class );
        for ( ModuleExportsStmt moduleExportsStmt : exportStatements ) {
            applyExportPatterns( moduleDeclaration, moduleExportsStmt );
            applyOpensPatterns( moduleDeclaration, moduleExportsStmt );
        }

        if ( moduleName != null ) {
            moduleDeclaration.setName( moduleName );
        }

        for (String usedService : uses) {
            moduleDeclaration.getModuleStmts().add( new ModuleUsesStmt( getType( usedService ) ) );
        }

        if ( addServiceUses ) {
            Set<String> usedServices = serviceLoaderUseScanner.getUsedServices( inputJar );
            for ( String usedService : usedServices ) {
                moduleDeclaration.getModuleStmts().add( new ModuleUsesStmt( getType( usedService ) ) );
            }
        }
    }

    private ModuleDeclaration applyExportPatterns(ModuleDeclaration moduleDeclaration, ModuleExportsStmt moduleExportsStmt) {
        boolean foundMatchingPattern = false;

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

                foundMatchingPattern = true;
                break;
            }
        }

        // remove export if not matched by any pattern
        if ( !foundMatchingPattern ) {
            moduleDeclaration.remove( moduleExportsStmt );
        }

        return moduleDeclaration;
    }

    private ModuleDeclaration applyOpensPatterns(ModuleDeclaration moduleDeclaration, ModuleExportsStmt moduleExportsStmt) {
        for (PackageNamePattern pattern : opensPatterns ) {
            if ( pattern.matches( moduleExportsStmt.getNameAsString() ) ) {
                if ( pattern.getKind() == Kind.INCLUSIVE ) {
                    ModuleOpensStmt moduleOpensStmt = new ModuleOpensStmt();
                    moduleOpensStmt.setName( moduleExportsStmt.getName() );

                    if ( !pattern.getTargetModules().isEmpty() ) {
                        for (String module : pattern.getTargetModules() ) {
                            moduleOpensStmt.getModuleNames().add( JavaParser.parseName( module ) );
                        }
                    }

                    moduleDeclaration.getModuleStmts().add( moduleOpensStmt );
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

    private Map<String, Boolean> generateModuleInfo() throws AssertionError {
        Map<String, Boolean> optionalityPerModule = new HashMap<>();

        List<String> command = new ArrayList<>();

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

            command.addAll( jdepsExtraArgs );
        }

        command.add( inputJar.toString() );

        log.debug( "Running jdeps " + String.join(  " ", command ) );
        jdeps.run( System.out, System.err, command.toArray( new String[0] ) );

        return optionalityPerModule;
    }

    private ModuleDeclaration parseGeneratedModuleInfo() {
        Path moduleDir = workingDirectory.resolve( autoModuleNameForInputJar );
        Path moduleInfo = moduleDir.resolve( "module-info.java" );

        return ModuleInfoCompiler.parseModuleInfo( moduleInfo );
    }

    private GeneratedModuleInfo writeModuleInfo(ModuleDeclaration moduleDeclaration) {
        Path outputModuleInfo = recreateDirectory( outputDirectory, moduleDeclaration.getNameAsString() )
                .resolve( "module-info.java" );

        try {
            Files.write( outputModuleInfo, moduleDeclaration.toString().getBytes() );

            log.info( "Created module descriptor at " + outputModuleInfo );
        }
        catch (IOException e) {
            throw new RuntimeException( "Couldn't write module-info.java", e );
        }

        return new GeneratedModuleInfo( moduleDeclaration.getNameAsString(), outputModuleInfo );
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
