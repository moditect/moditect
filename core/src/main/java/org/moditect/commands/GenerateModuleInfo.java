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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
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
import java.util.stream.Collectors;

import org.moditect.internal.analyzer.ServiceLoaderUseScanner;
import org.moditect.internal.compiler.ModuleInfoCompiler;
import org.moditect.internal.parser.JdepsExtraArgsExtractor;
import org.moditect.model.DependencePattern;
import org.moditect.model.DependencyDescriptor;
import org.moditect.model.GeneratedModuleInfo;
import org.moditect.model.PackageNamePattern;
import org.moditect.model.PackageNamePattern.Kind;
import org.moditect.spi.log.Log;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.modules.ModuleDeclaration;
import com.github.javaparser.ast.modules.ModuleExportsDirective;
import com.github.javaparser.ast.modules.ModuleOpensDirective;
import com.github.javaparser.ast.modules.ModuleRequiresDirective;
import com.github.javaparser.ast.modules.ModuleUsesDirective;
import com.github.javaparser.ast.modules.ModuleProvidesDirective;

import static com.github.javaparser.StaticJavaParser.parseName;

public class GenerateModuleInfo {

    private final Path inputJar;
    private final String autoModuleNameForInputJar;
    private final String moduleName;
    private final boolean open;
    private final Set<DependencyDescriptor> dependencies;
    private final List<PackageNamePattern> exportPatterns;
    private final List<PackageNamePattern> opensPatterns;
    private final List<DependencePattern> requiresPatterns;
    private final Set<String> opensResources;
    private final Set<String> uses;
    private final Set<String> provides;
    private final Path workingDirectory;
    private final Path outputDirectory;
    private final boolean addServiceUses;
    private final ServiceLoaderUseScanner serviceLoaderUseScanner;
    private final List<String> jdepsExtraArgs;
    private final Log log;
    private ToolProvider jdeps;

    public GenerateModuleInfo(
            Path inputJar, String moduleName, boolean open, 
            Set<DependencyDescriptor> dependencies, List<PackageNamePattern> exportPatterns,
            List<PackageNamePattern> opensPatterns, List<DependencePattern> requiresPatterns,
            Path workingDirectory, Path outputDirectory,
            Set<String> opensResources, Set<String> uses, Set<String> provides,
            boolean addServiceUses, List<String> jdepsExtraArgs, Log log
    ) {
        String autoModuleNameForInputJar = DependencyDescriptor.getAutoModuleNameFromInputJar(inputJar, null);

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
        this.opensResources = opensResources;
        this.uses = uses;
        this.provides = provides;
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

    public static Path createCopyWithAutoModuleNameManifestHeader(Path workingDirectory, Path inputJar, String moduleName) {
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

        List<ModuleRequiresDirective> requiresStatements = moduleDeclaration.findAll( ModuleRequiresDirective.class );

        for ( ModuleRequiresDirective moduleRequiresDirective : requiresStatements ) {
            if ( Boolean.TRUE.equals( optionalityPerModule.get( moduleRequiresDirective.getNameAsString() ) ) ) {
                moduleRequiresDirective.addModifier( Modifier.Keyword.STATIC );
            }

            // update any requires clauses to modules modularized with us to use the assigned module
            // name instead of the automatic module name
            for ( DependencyDescriptor dependency : dependencies ) {
                if ( dependency.getOriginalModuleName().equals( moduleRequiresDirective.getNameAsString() ) && dependency.getAssignedModuleName() != null ) {
                    moduleRequiresDirective.setName( dependency.getAssignedModuleName() );
                }
            }

            for ( DependencePattern dependence : requiresPatterns ) {
                if ( dependence.matches( moduleRequiresDirective.getNameAsString() ) ) {
                    if ( !dependence.isInclusive() ) {
                        moduleDeclaration.remove( moduleRequiresDirective );
                    }
                    if ( dependence.isMatchAll() && dependence.getModifiers().isEmpty() ) {
                        moduleRequiresDirective.removeModifier(Modifier.Keyword.TRANSITIVE );
                    }
                    else {
                        moduleRequiresDirective.getModifiers().clear();
                        dependence.getModifiers()
                            .stream()
                            .map( m -> Modifier.Keyword.valueOf( m.toUpperCase( Locale.ENGLISH ) ) )
                            .forEach( m -> moduleRequiresDirective.addModifier(m) );
                    }

                    break;
                }
            }
        }

        List<ModuleExportsDirective> exportStatements = moduleDeclaration.findAll( ModuleExportsDirective.class );
        for ( ModuleExportsDirective moduleExportsDirective : exportStatements ) {
            applyExportPatterns( moduleDeclaration, moduleExportsDirective );
            applyOpensPatterns( moduleDeclaration, moduleExportsDirective );
        }

        if ( moduleName != null ) {
            moduleDeclaration.setName( moduleName );
        }
        
        opensResources.stream().forEach(resourcePackage -> moduleDeclaration.getDirectives().add(
                new ModuleOpensDirective(parseName(resourcePackage), new NodeList<>())
        ));

        for (String usedService : uses) {
            moduleDeclaration.getDirectives().add( new ModuleUsesDirective(parseName(usedService)) );
        }

        provides.stream().map(
                providedService -> providedService.split("\\s+with\\s+")
        ).forEach(
                providedServiceArray -> moduleDeclaration.getDirectives().add(
                        new ModuleProvidesDirective(
                                parseName(providedServiceArray[0]),
                                NodeList.nodeList(
                                        Arrays.stream(
                                                providedServiceArray[1].split( "," )
                                        ).map( String::trim ).map(s -> parseName(s)).collect(
                                                Collectors.toSet()
                                        )
                                )
                        )
                )
        );

        if ( addServiceUses ) {
            Set<String> usedServices = serviceLoaderUseScanner.getUsedServices( inputJar );
            for ( String usedService : usedServices ) {
                moduleDeclaration.getDirectives().add( new ModuleUsesDirective(parseName(usedService)) );
            }
        }
    }

    private ModuleDeclaration applyExportPatterns(ModuleDeclaration moduleDeclaration, ModuleExportsDirective moduleExportsDirective) {
        boolean foundMatchingPattern = false;

        for (PackageNamePattern pattern : exportPatterns ) {
            if ( pattern.matches( moduleExportsDirective.getNameAsString() ) ) {
                if ( pattern.getKind() == Kind.INCLUSIVE ) {
                    if ( !pattern.getTargetModules().isEmpty() ) {
                        for (String module : pattern.getTargetModules() ) {
                            moduleExportsDirective.getModuleNames().add( parseName( module ) );
                        }
                    }
                }
                else {
                    moduleDeclaration.remove( moduleExportsDirective );
                }

                foundMatchingPattern = true;
                break;
            }
        }

        // remove export if not matched by any pattern
        if ( !foundMatchingPattern ) {
            moduleDeclaration.remove( moduleExportsDirective );
        }

        return moduleDeclaration;
    }

    private ModuleDeclaration applyOpensPatterns(ModuleDeclaration moduleDeclaration, ModuleExportsDirective moduleExportsDirective) {
        for (PackageNamePattern pattern : opensPatterns ) {
            if ( pattern.matches( moduleExportsDirective.getNameAsString() ) ) {
                if ( pattern.getKind() == Kind.INCLUSIVE ) {
                    ModuleOpensDirective moduleOpensDirective = new ModuleOpensDirective();
                    moduleOpensDirective.setName( moduleExportsDirective.getName() );

                    if ( !pattern.getTargetModules().isEmpty() ) {
                        for (String module : pattern.getTargetModules() ) {
                            moduleOpensDirective.getModuleNames().add( parseName( module ) );
                        }
                    }

                    moduleDeclaration.getDirectives().add( moduleOpensDirective );
                }

                break;
            }
        }

        return moduleDeclaration;
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
                String moduleName = DependencyDescriptor.getAutoModuleNameFromInputJar(dependency.getPath(), dependency.getAssignedModuleName());
                modules.append( moduleName );
                optionalityPerModule.put( moduleName, dependency.isOptional() );
                modulePath.append( dependency.getPath() );
            }

            command.add( "--add-modules" );
            command.add( modules.toString() );
            command.add( "--module-path" );
            command.add( modulePath.toString() );
        }

        command.addAll( jdepsExtraArgs );
        command.add( inputJar.toString() );

        log.debug( "Running jdeps " + String.join(  " ", command ) );
        int result = jdeps.run( System.out, System.err, command.toArray( new String[0] ) );

        if (result != 0) {
            throw new IllegalStateException("Invocation of jdeps failed: jdeps " + String.join(  " ", command ) );
        }

        return optionalityPerModule;
    }

    private ModuleDeclaration parseGeneratedModuleInfo() {
        Path moduleDir = workingDirectory.resolve( autoModuleNameForInputJar );
        Path moduleInfo = moduleDir.resolve("module-info.java");

        // JDK 11.0.11+ and 14+ put module-info.java in versions/<some-version>
        // so we check for that first.
        Optional<Integer> multiReleaseVersion = new JdepsExtraArgsExtractor(log).extractVersion(jdepsExtraArgs);
        if (multiReleaseVersion.isPresent()) {
            Path versionsModuleInfo = moduleDir.resolve("versions").resolve(multiReleaseVersion.get().toString())
                    .resolve("module-info.java");
            if (Files.exists(versionsModuleInfo)) {
                moduleInfo = versionsModuleInfo;
            }
        }

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
