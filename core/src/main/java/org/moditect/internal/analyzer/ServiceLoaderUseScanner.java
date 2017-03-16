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
package org.moditect.internal.analyzer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class ServiceLoaderUseScanner {

    public static Set<String> getUsedServices(Path jar) {
        Set<String> usedServices = new HashSet<>();

        try (JarFile jarFile = new JarFile( jar.toFile() ) ) {
            jarFile.stream()
                .filter( je -> !je.isDirectory() && je.getName().endsWith( ".class" ) )
                .forEach( je -> usedServices.addAll( getUsedServices( jarFile, je ) ) );
        }
        catch (IOException e) {
            throw new RuntimeException( "Couldn't open or close JAR file " + jar, e );
        }

        return usedServices;
    }

    private static Set<String> getUsedServices(JarFile jarFile, JarEntry je) {
        Set<String> usedServices = new HashSet<>();

        try ( InputStream classFile = jarFile.getInputStream( je ) ) {
            new ClassReader( classFile ).accept(
                    new ClassVisitor( Opcodes.ASM6 ) {

                        @Override
                        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {

                            return new MethodVisitor(Opcodes.ASM6) {

                                private Type lastType;

                                @Override
                                public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                                    if ( owner.equals( "java/util/ServiceLoader" ) && name.equals( "load" ) ) {
                                        usedServices.add( lastType.getClassName() );
                                    }
                                }

                                @Override
                                public void visitLdcInsn(Object cst) {
                                    if ( cst instanceof Type ) {
                                        lastType = (Type) cst;
                                    }
                                };
                            };
                        }
                    },
                    0
            );
        }
        catch (IOException e) {
            throw new RuntimeException( e );
        }

        return usedServices;
    }

}
