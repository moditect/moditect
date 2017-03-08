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
package org.moditect.compiler;

import static org.objectweb.asm.Opcodes.ACC_MANDATED;
import static org.objectweb.asm.Opcodes.ACC_MODULE;
import static org.objectweb.asm.Opcodes.ACC_STATIC_PHASE;
import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;
import static org.objectweb.asm.Opcodes.ACC_TRANSITIVE;
import static org.objectweb.asm.Opcodes.V1_9;

import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumSet;

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

public class ModuleInfoCompiler {

    public static ModuleDeclaration parseModuleInfo(Path moduleInfo) {
        CompilationUnit ast = null;

        try {
            ast = JavaParser.parse( moduleInfo );
        }
        catch (IOException e) {
            throw new RuntimeException( "Couldn't parse " + moduleInfo, e );
        }

        return ast.getModule()
            .orElseThrow( () -> new IllegalArgumentException( "Not a module-info.java: " + moduleInfo ) );
    }

    public static ModuleDeclaration parseModuleInfo(String moduleInfoSource) {
        CompilationUnit ast = JavaParser.parse( moduleInfoSource );

        return ast.getModule()
            .orElseThrow( () -> new IllegalArgumentException( "Not a module-info.java: " + moduleInfoSource ) );
    }

    public static byte[] compileModuleInfo(ModuleDeclaration module, String mainClass) {
        ClassWriter classWriter = new ClassWriter( 0 );
        classWriter.visit( V1_9, ACC_MODULE, "module-info", null, null, null );

        ModuleVisitor mv = classWriter.visitModule( module.getNameAsString(), ACC_SYNTHETIC, null );

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

        return classWriter.toByteArray();
    }

    private static int requiresModifiersAsInt(EnumSet<Modifier> modifiers) {
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
