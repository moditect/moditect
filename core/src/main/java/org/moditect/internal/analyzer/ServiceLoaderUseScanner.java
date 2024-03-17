/*
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Copyright The original authors
 *
 *  Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.moditect.internal.analyzer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.moditect.spi.log.Log;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class ServiceLoaderUseScanner {

    private final Log log;

    public ServiceLoaderUseScanner(Log log) {
        this.log = log;
    }

    public Set<String> getUsedServices(Path jar) {
        Set<String> usedServices = new HashSet<>();

        try (JarFile jarFile = new JarFile(jar.toFile())) {
            jarFile.stream()
                    .filter(je -> !je.isDirectory() && je.getName().endsWith(".class"))
                    .forEach(je -> usedServices.addAll(getUsedServices(jarFile, je)));
        }
        catch (IOException e) {
            throw new RuntimeException("Couldn't open or close JAR file " + jar, e);
        }

        return usedServices;
    }

    private Set<String> getUsedServices(JarFile jarFile, JarEntry je) {
        Set<String> usedServices = new HashSet<>();

        try (InputStream classFile = jarFile.getInputStream(je)) {
            new ClassReader(classFile).accept(
                    new ClassVisitor(Opcodes.ASM9) {

                        @Override
                        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {

                            return new MethodVisitor(Opcodes.ASM9) {

                                private Type lastType;

                                @Override
                                public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                                    if (owner.equals("java/util/ServiceLoader") && name.equals("load")) {
                                        if (lastType == null) {
                                            // TODO Log class/method
                                            log.warn("Cannot derive uses clause from service loader invocation with non constant class literal");
                                        }
                                        else {
                                            usedServices.add(lastType.getClassName());
                                        }
                                    }
                                }

                                @Override
                                public void visitLdcInsn(Object cst) {
                                    if (cst instanceof Type) {
                                        lastType = (Type) cst;
                                    }
                                };
                            };
                        }
                    },
                    0);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        return usedServices;
    }

}
