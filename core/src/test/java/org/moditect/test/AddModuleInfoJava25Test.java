/*
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Copyright The original authors
 *
 *  Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.moditect.test;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.moditect.commands.AddModuleInfo;
import org.moditect.internal.analyzer.ServiceLoaderUseScanner;
import org.moditect.spi.log.Log;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.ModuleVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AddModuleInfoJava25Test {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder(new File("target"));

    @Test
    public void addModuleInfoWithServiceUseFromJava25Class() throws Exception {
        // A numeric version keeps the regression compilable against the previous ASM dependency.
        assertServiceUseAndModuleInfo(69);
    }

    @Test
    public void addModuleInfoWithServiceUseFromJava8Class() throws Exception {
        assertServiceUseAndModuleInfo(Opcodes.V1_8);
    }

    private void assertServiceUseAndModuleInfo(int classVersion) throws Exception {
        byte[] originalClass = serviceUserClass(classVersion);
        assertEquals(classVersion, ((originalClass[6] & 0xff) << 8) | (originalClass[7] & 0xff));
        Path inputJar = temporaryFolder.newFile("service-user.jar").toPath();
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(inputJar))) {
            jar.putNextEntry(new JarEntry("com/example/ServiceUser.class"));
            jar.write(originalClass);
            jar.closeEntry();
        }

        Set<String> services = new ServiceLoaderUseScanner(new Log() {
            @Override
            public void debug(CharSequence message) {
            }

            @Override
            public void info(CharSequence message) {
            }

            @Override
            public void warn(CharSequence message) {
                throw new AssertionError(message);
            }

            @Override
            public void error(CharSequence message) {
                throw new AssertionError(message);
            }
        }).getUsedServices(inputJar);
        assertEquals(Collections.singleton("java.lang.Runnable"), services);

        Path output = temporaryFolder.newFolder("modules").toPath();
        new AddModuleInfo("module com.example { exports com.example; uses " + services.iterator().next() + "; }",
                null, null, inputJar, output, "9", false, null).run();

        try (JarFile jar = new JarFile(output.resolve(inputJar.getFileName()).toFile())) {
            JarEntry classEntry = jar.getJarEntry("com/example/ServiceUser.class");
            assertNotNull(classEntry);
            try (InputStream in = jar.getInputStream(classEntry)) {
                byte[] copiedClass = new byte[originalClass.length];
                int offset = 0;
                int read;
                while (offset < copiedClass.length && (read = in.read(copiedClass, offset, copiedClass.length - offset)) != -1) {
                    offset += read;
                }
                assertEquals(originalClass.length, offset);
                assertEquals(-1, in.read());
                assertArrayEquals(originalClass, copiedClass);
            }
            JarEntry descriptor = jar.getJarEntry("META-INF/versions/9/module-info.class");
            assertNotNull(descriptor);
            Set<String> moduleUses = new HashSet<>();
            try (InputStream in = jar.getInputStream(descriptor)) {
                new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9) {
                    @Override
                    public ModuleVisitor visitModule(String name, int access, String version) {
                        assertEquals("com.example", name);
                        return new ModuleVisitor(Opcodes.ASM9) {
                            @Override
                            public void visitUse(String service) {
                                moduleUses.add(service);
                            }
                        };
                    }
                }, 0);
            }
            assertEquals(Collections.singleton("java/lang/Runnable"), moduleUses);
        }
    }

    private static byte[] serviceUserClass(int classVersion) {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(classVersion, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER,
                "com/example/ServiceUser", null, "java/lang/Object", null);
        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "services", "()Ljava/util/ServiceLoader;", null, null);
        method.visitCode();
        method.visitLdcInsn(Type.getObjectType("java/lang/Runnable"));
        method.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/ServiceLoader", "load",
                "(Ljava/lang/Class;)Ljava/util/ServiceLoader;", false);
        method.visitInsn(Opcodes.ARETURN);
        method.visitMaxs(1, 0);
        method.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }
}
