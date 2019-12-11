/**
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
package org.moditect.internal.compiler;

import static com.github.javaparser.ParserConfiguration.LanguageLevel.*;
import static org.objectweb.asm.Opcodes.ACC_MANDATED;
import static org.objectweb.asm.Opcodes.ACC_MODULE;
import static org.objectweb.asm.Opcodes.ACC_OPEN;
import static org.objectweb.asm.Opcodes.ACC_STATIC_PHASE;
import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;
import static org.objectweb.asm.Opcodes.ACC_TRANSITIVE;
import static org.objectweb.asm.Opcodes.V9;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;

import com.github.javaparser.ast.nodeTypes.NodeWithModifiers;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.ModuleVisitor;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.modules.ModuleDeclaration;
import com.github.javaparser.ast.modules.ModuleExportsDirective;
import com.github.javaparser.ast.modules.ModuleOpensDirective;
import com.github.javaparser.ast.modules.ModuleProvidesDirective;
import com.github.javaparser.ast.modules.ModuleRequiresDirective;
import com.github.javaparser.ast.modules.ModuleUsesDirective;

public class ModuleInfoCompiler {

    static {
        JavaParser.getStaticConfiguration().setLanguageLevel(JAVA_9 );
    }

    public static ModuleDeclaration parseModuleInfo(Path moduleInfo) {
        CompilationUnit ast;

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

    public static byte[] compileModuleInfo(ModuleDeclaration module, String mainClass, String version) {
        ClassWriter classWriter = new ClassWriter( 0 );
        classWriter.visit( V9, ACC_MODULE, "module-info", null, null, null );

        int moduleAccess = module.isOpen() ? ACC_SYNTHETIC | ACC_OPEN : ACC_SYNTHETIC;
        ModuleVisitor mv = classWriter.visitModule( module.getNameAsString(), moduleAccess, version );

        if ( mainClass != null ) {
            mv.visitMainClass( getNameForBinary( mainClass ) );
        }

        for ( ModuleRequiresDirective requires : module.findAll( ModuleRequiresDirective.class ) ) {
            mv.visitRequire(
                requires.getName().asString(),
                requiresModifiersAsInt( requires ),
                null
            );
        }

        for ( ModuleExportsDirective export : module.findAll( ModuleExportsDirective.class ) ) {
            mv.visitExport(
                    getNameForBinary( export.getNameAsString() ),
                    0,
                    export.getModuleNames()
                        .stream()
                        .map( Name::toString )
                        .toArray( String[]::new )
            );
        }

        for ( ModuleProvidesDirective provides : module.findAll( ModuleProvidesDirective.class ) ) {
            mv.visitProvide(
                getNameForBinary( provides.getName() ),
                provides.getWith()
                    .stream()
                    .map( ModuleInfoCompiler::getNameForBinary )
                    .toArray( String[]::new )
            );
        }

        for ( ModuleUsesDirective uses : module.findAll( ModuleUsesDirective.class ) ) {
            mv.visitUse( getNameForBinary( uses.getName() ) );
        }

        for ( ModuleOpensDirective opens : module.findAll( ModuleOpensDirective.class ) ) {
            mv.visitOpen(
                    getNameForBinary( opens.getNameAsString() ),
                    0,
                    opens.getModuleNames()
                        .stream()
                        .map( Name::toString )
                        .toArray( String[]::new )
            );
        }

        mv.visitRequire( "java.base", ACC_MANDATED, null );
        mv.visitEnd();

        classWriter.visitEnd();

        return classWriter.toByteArray();
    }

    private static String getNameForBinary(Name name) {
        return getNameForBinary( name.asString() );
    }

    private static String getNameForBinary(String typeName) {
        Iterator<String> parts = Arrays.asList( typeName.split( "\\." ) ).iterator();
        StringBuilder typeNameForBinary = new StringBuilder();

        while ( parts.hasNext() ) {
            String part = parts.next();
            typeNameForBinary.append( part );

            // if the current part is upper-case, we assume it's a class and the following part is a nested class
            // that's as good as it gets without fully resolving all the type names against the module's classes
            if ( parts.hasNext() ) {
                if ( Character.isUpperCase( part.charAt(0) ) ) {
                    typeNameForBinary.append( "$" );
                }
                else {
                    typeNameForBinary.append( "/" );
                }
            }
        }

        return typeNameForBinary.toString();
    }

    private static int requiresModifiersAsInt(NodeWithModifiers<?> modifiers) {
        int result = 0;

        if ( modifiers.hasModifier( Modifier.Keyword.STATIC ) ) {
            result |= ACC_STATIC_PHASE;
        }
        if ( modifiers.hasModifier( Modifier.Keyword.TRANSITIVE ) ) {
            result |= ACC_TRANSITIVE;
        }

        return result;
    }
}
