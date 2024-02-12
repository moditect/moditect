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
package org.moditect.commands;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.moditect.internal.compiler.ModuleInfoCompiler;

import com.github.javaparser.ast.modules.ModuleDeclaration;

import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

/**
 * Creates a copy of a given JAR file, adding a module-info.class descriptor.
 *
 * @author Gunnar Morling
 */
public class AddModuleInfo {

    private static final int DEFAULT_BUFFER_SIZE = 8192;
    private static final String NO_JVM_VERSION = "base";
    private static final String MANIFEST_ENTRY_NAME = "META-INF/MANIFEST.MF";
    private static final String MODULE_INFO_CLASS = "module-info.class";

    private final String moduleInfoSource;
    private final String mainClass;
    private final String version;
    private final Path inputJar;
    private final Path outputDirectory;
    private final Integer jvmVersion;
    private final boolean overwriteExistingFiles;
    private final Instant timestamp;

    public AddModuleInfo(String moduleInfoSource, String mainClass, String version, Path inputJar, Path outputDirectory, String jvmVersion,
                         boolean overwriteExistingFiles, Instant timestamp) {
        this.moduleInfoSource = moduleInfoSource;
        this.mainClass = mainClass;
        this.version = version;
        this.inputJar = inputJar;
        this.outputDirectory = outputDirectory;

        // #67 It'd be nice to use META-INF/services/9 by default to avoid conflicts with legacy
        // classpath scanners, but this causes issues with subsequent jdeps invocations if there
        // are MR-JARs and non-MR JARs passed to it due to https://bugs.openjdk.java.net/browse/JDK-8207162
        if (jvmVersion == null || jvmVersion.equals(NO_JVM_VERSION)) {
            this.jvmVersion = null;
        }
        else {
            try {
                this.jvmVersion = Integer.valueOf(jvmVersion);
                if (this.jvmVersion < 9) {
                    throw new NumberFormatException();
                }
            }
            catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid JVM Version: " + jvmVersion + ". Allowed values are 'base' and integer values >= 9.");
            }
        }
        this.overwriteExistingFiles = overwriteExistingFiles;
        this.timestamp = timestamp;
    }

    public void run() {
        if (Files.isDirectory(inputJar)) {
            throw new IllegalArgumentException("Input JAR must not be a directory");
        }

        if (!Files.exists(outputDirectory)) {
            throw new IllegalArgumentException("Output directory doesn't exist: " + outputDirectory);
        }

        Path outputJar = outputDirectory.resolve(inputJar.getFileName());

        if (Files.exists(outputJar) && !overwriteExistingFiles) {
            throw new RuntimeException(
                    "File " + outputJar + " already exists; either set 'overwriteExistingFiles' to true or specify another output directory");
        }

        ModuleDeclaration module = ModuleInfoCompiler.parseModuleInfo(moduleInfoSource);
        byte[] clazz = ModuleInfoCompiler.compileModuleInfo(module, mainClass, version);

        try {
            Files.createDirectories(outputJar.toAbsolutePath().getParent());
            Files.createFile(outputJar.toAbsolutePath());
        }
        catch (IOException e) {
            throw new RuntimeException("Couldn't copy JAR file", e);
        }

        boolean versionedModuleInfo = jvmVersion != null;
        String versionedModuleInfoClass = "META-INF/versions/" + jvmVersion + "/" + MODULE_INFO_CLASS;
        long lastModifiedTime = toFileTime(timestamp).toMillis();

        // brute force copy all entries
        try (JarFile jarFile = new JarFile(inputJar.toAbsolutePath().toFile());
                JarOutputStream jarout = new JarOutputStream(Files.newOutputStream(outputJar.toAbsolutePath(), TRUNCATE_EXISTING))) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry inputEntry = entries.nextElement();

                // manifest requires extra care due to MRJARs
                if (MANIFEST_ENTRY_NAME.equals(inputEntry.getName()) && versionedModuleInfo) {
                    Manifest manifest = jarFile.getManifest();
                    if (null == manifest) {
                        manifest = new Manifest();
                        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
                    }
                    manifest.getMainAttributes().put(new Attributes.Name("Multi-Release"), "true");
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    manifest.write(baos);

                    JarEntry outputEntry = new JarEntry(inputEntry.getName());
                    outputEntry.setTime(lastModifiedTime);
                    jarout.putNextEntry(outputEntry);
                    jarout.write(baos.toByteArray(), 0, baos.size());
                    jarout.closeEntry();
                }
                else if ((MODULE_INFO_CLASS.equals(inputEntry.getName()) && !versionedModuleInfo) ||
                        (versionedModuleInfoClass.equals(inputEntry.getName()) && versionedModuleInfo)) {
                    // skip this entry as we'll overwrite it
                }
                else {
                    // copy entry as is, set timestamp
                    JarEntry outputEntry = new JarEntry(inputEntry.getName());
                    outputEntry.setTime(lastModifiedTime);
                    jarout.putNextEntry(outputEntry);
                    copy(jarFile.getInputStream(inputEntry), jarout);
                    jarout.closeEntry();
                }
            }

            // copy module descriptor
            JarEntry outputEntry = versionedModuleInfo ? new JarEntry(versionedModuleInfoClass) : new JarEntry(MODULE_INFO_CLASS);
            outputEntry.setTime(lastModifiedTime);
            jarout.putNextEntry(outputEntry);
            jarout.write(clazz, 0, clazz.length);
            jarout.closeEntry();
        }
        catch (IOException e) {
            throw new RuntimeException("Couldn't add module-info.class to JAR", e);
        }
    }

    private FileTime toFileTime(Instant timestamp) {
        return FileTime.from(timestamp != null ? timestamp : Instant.now());
    }

    private void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int read;
        while ((read = in.read(buffer, 0, DEFAULT_BUFFER_SIZE)) >= 0) {
            out.write(buffer, 0, read);
        }
    }
}
