/*
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Copyright The original authors
 *
 *  Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
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
import java.nio.file.StandardCopyOption;
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
    private static final String JAVA_BIN = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";

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
        Path inputJar = prepareTestJar();

        ProcessBuilder builder = new ProcessBuilder(
                JAVA_BIN, "--module-path", inputJar.toString(), "--module", "com.example")
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
                inputJar,
                GENERATED_TEST_MODULES,
                "9",
                false,
                null)
                .run();

        Path outputJar = GENERATED_TEST_MODULES.resolve(inputJar.getFileName());
        builder = new ProcessBuilder(
                JAVA_BIN, "--module-path", outputJar.toString(), "--module", "com.example");

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
    public void addJvmVersionModuleInfoTwiceAndRunModular() throws Exception {
        Path inputJar1 = prepareTestJar();

        ProcessBuilder builder = new ProcessBuilder(
                JAVA_BIN, "--module-path", inputJar1.toString(), "--module", "com.example")
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
                inputJar1,
                GENERATED_TEST_MODULES,
                "9",
                false,
                null)
                .run();

        Path outputJar1 = GENERATED_TEST_MODULES.resolve(inputJar1.getFileName());
        Path inputJar2 = GENERATED_TEST_RESOURCES.resolve("example2.jar");
        Files.copy(
                outputJar1,
                inputJar2,
                StandardCopyOption.REPLACE_EXISTING);

        new AddModuleInfo(
                "module com.example {}",
                "com.example.HelloWorld",
                "1.42.3",
                inputJar2,
                GENERATED_TEST_MODULES,
                "9",
                false,
                null)
                .run();

        Path outputJar2 = GENERATED_TEST_MODULES.resolve(inputJar2.getFileName());
        builder = new ProcessBuilder(
                JAVA_BIN, "--module-path", outputJar2.toString(), "--module", "com.example");

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
        Path inputJar = prepareTestJar();

        ProcessBuilder builder = new ProcessBuilder(
                JAVA_BIN, "--module-path", inputJar.toString(), "--module", "com.example")
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
                inputJar,
                GENERATED_TEST_MODULES,
                null,
                false,
                null)
                .run();

        Path outputJar = GENERATED_TEST_MODULES.resolve(inputJar.getFileName());
        builder = new ProcessBuilder(
                JAVA_BIN, "--module-path", outputJar.toString(), "--module", "com.example");

        process = builder.start();
        process.waitFor();

        if (process.exitValue() != 0) {
            throw new AssertionError();
        }
    }

    @Test
    public void addModuleInfoTwiceAndRunModular() throws Exception {
        Path inputJar1 = prepareTestJar();

        ProcessBuilder builder = new ProcessBuilder(
                JAVA_BIN, "--module-path", inputJar1.toString(), "--module", "com.example")
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
                inputJar1,
                GENERATED_TEST_MODULES,
                null,
                false,
                null)
                .run();

        Path outputJar1 = GENERATED_TEST_MODULES.resolve(inputJar1.getFileName());
        Path inputJar2 = GENERATED_TEST_RESOURCES.resolve("example2.jar");
        Files.copy(
                outputJar1,
                inputJar2,
                StandardCopyOption.REPLACE_EXISTING);

        new AddModuleInfo(
                "module com.example {}",
                "com.example.HelloWorld",
                "1.42.3",
                inputJar2,
                GENERATED_TEST_MODULES,
                null,
                false,
                null)
                .run();

        Path outputJar2 = GENERATED_TEST_MODULES.resolve(inputJar2.getFileName());
        builder = new ProcessBuilder(
                JAVA_BIN, "--module-path", outputJar2.toString(), "--module", "com.example");

        process = builder.start();
        process.waitFor();

        if (process.exitValue() != 0) {
            throw new AssertionError();
        }
    }

    private Path prepareTestJar() throws Exception {
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

        try (JarOutputStream target = new JarOutputStream(new FileOutputStream(exampleJar.toFile()), manifest)) {

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
        }

        return exampleJar;
    }
}
