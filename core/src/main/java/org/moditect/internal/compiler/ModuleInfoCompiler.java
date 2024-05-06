/*
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Copyright The original authors
 *
 *  Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.moditect.internal.compiler;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.ModuleVisitor;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.modules.ModuleDeclaration;
import com.github.javaparser.ast.modules.ModuleExportsDirective;
import com.github.javaparser.ast.modules.ModuleOpensDirective;
import com.github.javaparser.ast.modules.ModuleProvidesDirective;
import com.github.javaparser.ast.modules.ModuleRequiresDirective;
import com.github.javaparser.ast.modules.ModuleUsesDirective;
import com.github.javaparser.ast.nodeTypes.NodeWithModifiers;

import static com.github.javaparser.ParserConfiguration.LanguageLevel.*;
import static org.objectweb.asm.Opcodes.ACC_MANDATED;
import static org.objectweb.asm.Opcodes.ACC_MODULE;
import static org.objectweb.asm.Opcodes.ACC_OPEN;
import static org.objectweb.asm.Opcodes.ACC_STATIC_PHASE;
import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;
import static org.objectweb.asm.Opcodes.ACC_TRANSITIVE;
import static org.objectweb.asm.Opcodes.V9;

public class ModuleInfoCompiler {

    static {
        StaticJavaParser.getConfiguration().setLanguageLevel(JAVA_9);
    }

    public static ModuleDeclaration parseModuleInfo(Path moduleInfo) {
        CompilationUnit ast;

        try {
            ast = StaticJavaParser.parse(moduleInfo);
        }
        catch (IOException e) {
            throw new RuntimeException("Couldn't parse " + moduleInfo, e);
        }

        return ast.getModule()
                .orElseThrow(() -> new IllegalArgumentException("Not a module-info.java: " + moduleInfo));
    }

    public static ModuleDeclaration parseModuleInfo(String moduleInfoSource) {
        CompilationUnit ast = StaticJavaParser.parse(moduleInfoSource);

        return ast.getModule()
                .orElseThrow(() -> new IllegalArgumentException("Not a module-info.java: " + moduleInfoSource));
    }

    public static byte[] compileModuleInfo(ModuleDeclaration module, String mainClass, String version) {
        ClassWriter classWriter = new ClassWriter(0);
        classWriter.visit(V9, ACC_MODULE, "module-info", null, null, null);

        int moduleAccess = module.isOpen() ? ACC_SYNTHETIC | ACC_OPEN : ACC_SYNTHETIC;
        ModuleVisitor mv = classWriter.visitModule(module.getNameAsString(), moduleAccess, version);

        if (mainClass != null) {
            mv.visitMainClass(getNameForBinary(mainClass, Kind.CLASS));
        }

        for (ModuleRequiresDirective requires : module.findAll(ModuleRequiresDirective.class)) {
            mv.visitRequire(
                    requires.getName().asString(),
                    requiresModifiersAsInt(requires),
                    null);
        }

        for (ModuleExportsDirective export : module.findAll(ModuleExportsDirective.class)) {
            mv.visitExport(
                    getNameForBinary(export.getNameAsString(), Kind.PACKAGE),
                    0,
                    export.getModuleNames()
                            .stream()
                            .map(Name::toString)
                            .toArray(String[]::new));
        }

        for (ModuleProvidesDirective provides : module.findAll(ModuleProvidesDirective.class)) {
            mv.visitProvide(
                    getNameForBinary(provides.getName(), Kind.CLASS),
                    provides.getWith()
                            .stream()
                            .map(name -> getNameForBinary(name, Kind.CLASS))
                            .toArray(String[]::new));
        }

        for (ModuleUsesDirective uses : module.findAll(ModuleUsesDirective.class)) {
            mv.visitUse(getNameForBinary(uses.getName(), Kind.CLASS));
        }

        for (ModuleOpensDirective opens : module.findAll(ModuleOpensDirective.class)) {
            mv.visitOpen(
                    getNameForBinary(opens.getNameAsString(), Kind.PACKAGE),
                    0,
                    opens.getModuleNames()
                            .stream()
                            .map(Name::toString)
                            .toArray(String[]::new));
        }

        mv.visitRequire("java.base", ACC_MANDATED, null);
        mv.visitEnd();

        classWriter.visitEnd();

        return classWriter.toByteArray();
    }

    private static String getNameForBinary(Name name, Kind kind) {
        return getNameForBinary(name.asString(), kind);
    }

    private enum Kind {
        CLASS,
        PACKAGE
    }

    private static String getNameForBinary(String typeName, Kind kind) {
        Iterator<String> parts = Arrays.asList(typeName.split("\\.")).iterator();
        StringBuilder typeNameForBinary = new StringBuilder();

        while (parts.hasNext()) {
            String part = parts.next();
            typeNameForBinary.append(part);

            // if the current part is upper-case, we assume it's a class and the following part is a nested class
            // that's as good as it gets without fully resolving all the type names against the module's classes
            if (parts.hasNext()) {
                if (kind == Kind.CLASS && Character.isUpperCase(part.charAt(0))) {
                    typeNameForBinary.append("$");
                }
                else {
                    typeNameForBinary.append("/");
                }
            }
        }

        return typeNameForBinary.toString();
    }

    private static int requiresModifiersAsInt(NodeWithModifiers<?> modifiers) {
        int result = 0;

        if (modifiers.hasModifier(Modifier.Keyword.STATIC)) {
            result |= ACC_STATIC_PHASE;
        }
        if (modifiers.hasModifier(Modifier.Keyword.TRANSITIVE)) {
            result |= ACC_TRANSITIVE;
        }

        return result;
    }
}
