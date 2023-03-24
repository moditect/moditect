/*
 *  Copyright 2017 - 2023 The ModiTect authors
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
package org.moditect.test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

import org.junit.Before;
import org.junit.Test;
import org.moditect.commands.AddModuleInfo;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;

/**
 * @author Gunnar Morling
 */
public class AddModuleInfoTest {

    private static final Path GENERATED_TEST_RESOURCES = Paths.get("target", "generated-test-resources");
    private static final Path GENERATED_TEST_MODULES = Paths.get("target", "generated-test-modules");

    @Before
    public void prepareDirectories() throws Exception {
        truncateFolder(GENERATED_TEST_RESOURCES);
        truncateFolder(GENERATED_TEST_MODULES);
    }

    private void truncateFolder(Path folder) throws Exception {
        if (Files.exists(folder)) {
            Files.walkFileTree(folder, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                    Files.deleteIfExists(path);
                    return super.visitFile(path, attrs);
                }
            });
            Files.deleteIfExists(folder);
        }
        Files.createDirectory(folder);
    }

    @Test
    public void addJvmVersionModuleInfoAndRunModular() throws Exception {
        prepareTestJar();

        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome +
                File.separator + "bin" +
                File.separator + "java";

        ProcessBuilder builder = new ProcessBuilder(
                javaBin, "--module-path", GENERATED_TEST_RESOURCES + File.separator + "example.jar", "--module", "com.example")
                .redirectOutput(Redirect.INHERIT);

        Process process = builder.start();
        process.waitFor();

        if (process.exitValue() == 0) {
            throw new AssertionError();
        }

        new AddModuleInfo(
                "module com.example {}",
                "com.example.HelloWorld",
                "1.42.3",
                Paths.get("target", "generated-test-resources", "example.jar"),
                Paths.get("target", "generated-test-modules"),
                "9",
                false,
                null)
                .run();

        builder = new ProcessBuilder(
                javaBin, "--module-path", GENERATED_TEST_MODULES + File.separator + "example.jar", "--module", "com.example");

        process = builder.start();
        process.waitFor();

        if (process.exitValue() != 0) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            process.getInputStream().transferTo(baos);
            process.getErrorStream().transferTo(baos);
            throw new AssertionError(baos.toString());
        }
    }

    @Test
    public void addModuleInfoAndRunModular() throws Exception {
        prepareTestJar();

        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome +
                File.separator + "bin" +
                File.separator + "java";

        ProcessBuilder builder = new ProcessBuilder(
                javaBin, "--module-path", GENERATED_TEST_RESOURCES + File.separator + "example.jar", "--module", "com.example")
                .redirectOutput(Redirect.INHERIT);

        Process process = builder.start();
        process.waitFor();

        if (process.exitValue() == 0) {
            throw new AssertionError();
        }

        new AddModuleInfo(
                "module com.example {}",
                "com.example.HelloWorld",
                "1.42.3",
                Paths.get("target", "generated-test-resources", "example.jar"),
                Paths.get("target", "generated-test-modules"),
                null,
                false,
                null)
                .run();

        builder = new ProcessBuilder(
                javaBin, "--module-path", GENERATED_TEST_MODULES + File.separator + "example.jar", "--module", "com.example");

        process = builder.start();
        process.waitFor();

        if (process.exitValue() != 0) {
            throw new AssertionError();
        }
    }

    private void prepareTestJar() throws Exception {
        Compilation compilation = Compiler.javac()
                .compile(
                        JavaFileObjects.forSourceString(
                                "com.example.HelloWorld",
                                "package com.example;" +
                                        "public class HelloWorld {" +
                                        "    public static void main(String... args) {" +
                                        "        System.out.println( \"Moin\" );" +
                                        "    }" +
                                        "}"));

        Optional<JavaFileObject> classFile = compilation.generatedFile(
                StandardLocation.CLASS_OUTPUT, "com/example/HelloWorld.class");

        Path exampleJar = GENERATED_TEST_RESOURCES.resolve("example.jar");

        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");

        JarOutputStream target = new JarOutputStream(new FileOutputStream(exampleJar.toFile()), manifest);

        long now = System.currentTimeMillis();
        JarEntry entry = new JarEntry("com/");
        entry.setTime(now);
        target.putNextEntry(entry);
        target.closeEntry();

        entry = new JarEntry("com/example/");
        entry.setTime(now);
        target.putNextEntry(entry);
        target.closeEntry();

        entry = new JarEntry("com/example/HelloWorld.class");
        entry.setTime(now);
        target.putNextEntry(entry);

        try (InputStream is = classFile.get().openInputStream()) {
            byte[] bytes = is.readAllBytes();
            target.write(bytes, 0, bytes.length);
        }

        target.closeEntry();

        target.close();
    }
}
