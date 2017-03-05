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

import static org.objectweb.asm.Opcodes.ACC_MANDATED;
import static org.objectweb.asm.Opcodes.ACC_MODULE;
import static org.objectweb.asm.Opcodes.ACC_STATIC_PHASE;
import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;
import static org.objectweb.asm.Opcodes.ACC_TRANSITIVE;
import static org.objectweb.asm.Opcodes.V1_9;

import java.io.IOException;
import java.lang.module.ModuleDescriptor;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.ModuleVisitor;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.modules.ModuleDeclaration;
import com.github.javaparser.ast.modules.ModuleExportsStmt;
import com.github.javaparser.ast.modules.ModuleProvidesStmt;
import com.github.javaparser.ast.modules.ModuleRequiresStmt;
import com.github.javaparser.ast.modules.ModuleUsesStmt;
import com.github.javaparser.ast.type.Type;

/**
 * Creates a copy of a given JAR file, adding a module-info.class descriptor.
 *
 * @author Gunnar Morling
 */
public class AddModuleInfo {

    private final String moduleInfoSource;
    private final String mainClass;
    private final Path inputJar;
    private final Path outputDirectory;

    public AddModuleInfo(String moduleInfoSource, String mainClass, Path inputJar, Path outputDirectory) {
        this.moduleInfoSource = moduleInfoSource;
        this.mainClass = mainClass;
        this.inputJar = inputJar;
        this.outputDirectory = outputDirectory;
    }

    public void run() {
        if ( Files.isDirectory( inputJar ) ) {
            throw new IllegalArgumentException( "Input JAR must not be a directory" );
        }

        if ( !Files.exists( outputDirectory ) ) {
            throw new IllegalArgumentException( "Output directory doesn't exist: "  + outputDirectory);
        }

        Path outputJar = outputDirectory.resolve( inputJar.getFileName() );

        try {
            Files.copy( inputJar, outputJar );
        }
        catch(IOException e) {
            throw new RuntimeException( "Couldn't copy JAR file", e );
        }

        CompilationUnit ast = JavaParser.parse( moduleInfoSource );
        ModuleDeclaration module = ast.getModule().orElseThrow( () -> new IllegalArgumentException( "Not a module-info.java: " + moduleInfoSource ) );

        ModuleDescriptor descriptor = ModuleDescriptor.newModule( module.getNameAsString() )
                .build();

        ClassWriter classWriter = new ClassWriter( 0 );
        classWriter.visit( V1_9, ACC_MODULE, "module-info", null, null, null );

        ModuleVisitor mv = classWriter.visitModule( descriptor.name(), ACC_SYNTHETIC, null );

        if ( mainClass != null ) {
            mv.visitMainClass( mainClass.replace( '.', '/' ) );
        }

        for ( ModuleRequiresStmt requires : module.getNodesByType( ModuleRequiresStmt.class ) ) {
            mv.visitRequire(
                requires.getName().asString(),
                requiresModifiersAsInt( requires.getModifiers() ),
                null
            );
        }

        for ( ModuleExportsStmt export : module.getNodesByType( ModuleExportsStmt.class ) ) {
            mv.visitExport( export.getName().asString().replace( '.', '/' ), 0 );
        }

        for ( ModuleProvidesStmt provides : module.getNodesByType( ModuleProvidesStmt.class ) ) {
            mv.visitProvide(
                provides.getType().toString().replace( '.', '/' ),
                provides.getWithTypes()
                    .stream()
                    .map( Type::toString )
                    .map( s -> s.replace( '.', '/' ) )
                    .toArray( String[]::new )
            );
        }

        for ( ModuleUsesStmt uses : module.getNodesByType( ModuleUsesStmt.class ) ) {
            mv.visitUse( uses.getType().toString().replace( '.', '/' ) );
        }

        mv.visitRequire( "java.base", ACC_MANDATED, null );
        mv.visitEnd();

        classWriter.visitEnd();

        Map<String, String> env = new HashMap<>();
        env.put( "create", "true" );
        // locate file system by using the syntax
        // defined in java.net.JarURLConnection
        URI uri = URI.create( "jar:" + outputJar.toUri().toString() );

       try (FileSystem zipfs = FileSystems.newFileSystem( uri, env ) ) {
           Files.write( zipfs.getPath( "module-info.class" ), classWriter.toByteArray() );
        }
       catch(IOException e) {
            throw new RuntimeException( "Couldn't add module-info.class to JAR", e );
        }
    }

    private int requiresModifiersAsInt(EnumSet<Modifier> modifiers) {
        int result = 0;

        if ( modifiers.contains( Modifier.STATIC ) ) {
            result |= ACC_STATIC_PHASE;
        }
        if ( modifiers.contains( Modifier.TRANSITIVE ) ) {
            result |= ACC_TRANSITIVE;
        }

        return result;
    }
}
