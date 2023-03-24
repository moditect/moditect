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
package org.moditect.mavenplugin.add;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ExclusionArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.model.Exclusion;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.moditect.commands.AddModuleInfo;
import org.moditect.internal.compiler.ModuleInfoCompiler;
import org.moditect.mavenplugin.add.model.MainModuleConfiguration;
import org.moditect.mavenplugin.add.model.ModuleConfiguration;
import org.moditect.mavenplugin.common.model.ArtifactConfiguration;
import org.moditect.mavenplugin.generate.ModuleInfoGenerator;
import org.moditect.mavenplugin.generate.model.ArtifactIdentifier;
import org.moditect.mavenplugin.util.ArtifactResolutionHelper;
import org.moditect.model.DependencyDescriptor;
import org.moditect.model.GeneratedModuleInfo;

/**
 * @author Gunnar Morling
 */
@Mojo(name = "add-module-info", defaultPhase = LifecyclePhase.PROCESS_RESOURCES, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class AddModuleInfoMojo extends AbstractMojo {

    private static final String MODULE_INFO_CLASS = "module-info.class";

    private static final Pattern VERSIONED_MODULE_INFO = Pattern.compile("META-INF/versions/\\d+/" + MODULE_INFO_CLASS);

    @Component
    private RepositorySystem repoSystem;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true, required = true)
    private RepositorySystemSession repoSession;

    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true, required = true)
    private List<RemoteRepository> remoteRepos;

    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    @Parameter(defaultValue = "${project.artifactId}", readonly = true, required = true)
    private String artifactId;

    @Parameter(defaultValue = "${project.version}", readonly = true, required = true)
    private String version;

    @Parameter(property = "moditect.jvmVersion")
    private String jvmVersion;

    @Parameter(readonly = true, defaultValue = "${project.build.directory}/moditect")
    private File workingDirectory;

    @Parameter(readonly = true, defaultValue = "${project.build.directory}")
    private File buildDirectory;

    @Parameter(property = "outputDirectory", defaultValue = "${project.build.directory}/modules")
    private File outputDirectory;

    @Parameter(property = "overwriteExistingFiles", defaultValue = "false")
    private boolean overwriteExistingFiles;

    /**
     * Timestamp for reproducible output archive entries, either formatted as ISO 8601 extended offset date-time
     * (e.g. in UTC such as '2011-12-03T10:15:30Z' or with an offset '2019-10-05T20:37:42+06:00'),
     * or as an int representing seconds since the epoch
     * (like <a href="https://reproducible-builds.org/docs/source-date-epoch/">SOURCE_DATE_EPOCH</a>).
     */
    @Parameter(defaultValue = "${project.build.outputTimestamp}")
    private String outputTimestamp;

    @Parameter(property = "moditect.skip", defaultValue = "false")
    private boolean skip;

    @Parameter(property = "moditect.failOnWarning", defaultValue = "true")
    private boolean failOnWarning;

    @Parameter
    private MainModuleConfiguration module;

    @Parameter
    private List<ModuleConfiguration> modules;

    @Parameter
    private List<Exclusion> exclusions;

    @Parameter
    private List<String> jdepsExtraArgs;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        // Check if this plugin should be skipped
        if (skip) {
            getLog().debug("Mojo 'add-module-info' skipped by configuration");
            return;
        }
        // Don't try to run this plugin, when packaging type is 'pom'
        // (may be better to only run it on specific packaging types, like 'jar')
        if (project.getModel().getPackaging().equalsIgnoreCase("pom")) {
            getLog().debug("Mojo 'add-module-info' not executed on packaging type '" + project.getModel().getPackaging() + "'");
            return;
        }

        if (exclusions != null) {
            ArtifactFilter scopeFilter = new ScopeArtifactFilter("compile+runtime");
            ArtifactFilter exclusionFilter = new ExclusionArtifactFilter(exclusions);
            project.setArtifactFilter(new AndArtifactFilter(Arrays.asList(scopeFilter, exclusionFilter)));
        }

        Path outputPath = outputDirectory.toPath();

        createDirectories();

        ArtifactResolutionHelper artifactResolutionHelper = new ArtifactResolutionHelper(repoSystem, repoSession, remoteRepos);

        ModuleInfoGenerator moduleInfoGenerator = new ModuleInfoGenerator(
                project, repoSystem, repoSession, remoteRepos, artifactResolutionHelper, jdepsExtraArgs, getLog(), workingDirectory,
                new File(workingDirectory, "generated-sources"));

        resolveArtifactsToBeModularized(artifactResolutionHelper);

        Map<ArtifactIdentifier, String> assignedNamesByModule = getAssignedModuleNamesByModule(artifactResolutionHelper);
        Map<ArtifactIdentifier, Path> modularizedJars = new HashMap<>();

        if (modules != null) {
            for (ModuleConfiguration moduleConfiguration : modules) {
                Path inputFile = getInputFile(moduleConfiguration, artifactResolutionHelper);
                if (isModularJar(inputFile)) {
                    String message = "File " + inputFile.getFileName() + " is already modular";
                    if (failOnWarning) {
                        throw new MojoExecutionException(message);
                    }
                    else {
                        getLog().warn(message);
                        continue;
                    }
                }

                String moduleInfoSource = getModuleInfoSource(inputFile, moduleConfiguration, moduleInfoGenerator, assignedNamesByModule, modularizedJars);

                AddModuleInfo addModuleInfo = new AddModuleInfo(
                        moduleInfoSource,
                        moduleConfiguration.getMainClass(),
                        getVersion(moduleConfiguration),
                        inputFile,
                        outputPath,
                        jvmVersion,
                        overwriteExistingFiles,
                        InstantConverter.convert(outputTimestamp));

                addModuleInfo.run();

                if (moduleConfiguration.getArtifact() != null) {
                    modularizedJars.put(
                            new ArtifactIdentifier(moduleConfiguration.getResolvedArtifact()),
                            outputPath.resolve(inputFile.getFileName()));
                }
            }
        }

        if (module != null) {
            Path inputJar = buildDirectory.toPath().resolve(project.getModel().getBuild().getFinalName() + ".jar");
            if (!Files.exists(inputJar)) {
                throw new MojoExecutionException("Couldn't find file " + inputJar + ". Run this goal for the project's JAR only after the maven-jar-plugin.");
            }

            if (isModularJar(inputJar)) {
                String message = "File " + inputJar.getFileName() + " is already modular";
                if (failOnWarning) {
                    throw new MojoExecutionException(message);
                }
                else {
                    getLog().warn(message);
                    return;
                }
            }

            AddModuleInfo addModuleInfo = new AddModuleInfo(
                    getModuleInfoSource(inputJar, module, moduleInfoGenerator, assignedNamesByModule, modularizedJars),
                    module.getMainClass(),
                    version,
                    inputJar,
                    outputPath,
                    jvmVersion,
                    overwriteExistingFiles,
                    InstantConverter.convert(outputTimestamp));
            addModuleInfo.run();

            try {
                Files.copy(outputPath.resolve(inputJar.getFileName()), inputJar, StandardCopyOption.REPLACE_EXISTING);
            }
            catch (IOException e) {
                throw new RuntimeException("Couldn't replace " + inputJar + " with modularized version", e);
            }
        }
    }

    /**
     * Determines if the given JAR is already modular that is,
     * it contains either a "module-info.class" entry or at least
     * one "/META-INF/version/\\d+/module-info.class" entry.
     */
    private boolean isModularJar(Path jarPath) {
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();
                if (entryName.equals(MODULE_INFO_CLASS) || VERSIONED_MODULE_INFO.matcher(entryName).matches()) {
                    return true;
                }
            }
        }
        catch (IOException ioe) {
            throw new IllegalStateException("Unexpected error when reading " + jarPath.getFileName());
        }
        return false;
    }

    /**
     * Sets the resolved artifact of all artifact configurations. If no version is given, we try
     * to obtain it from the project dependencies.
     */
    private void resolveArtifactsToBeModularized(ArtifactResolutionHelper artifactResolutionHelper) throws MojoExecutionException {
        if (modules == null) {
            return;
        }

        for (ModuleConfiguration moduleConfiguration : modules) {
            ArtifactConfiguration artifact = moduleConfiguration.getArtifact();

            if (artifact != null) {
                if (artifact.getVersion() == null) {
                    artifact.setVersion(determineVersion(artifact));
                }

                moduleConfiguration.setResolvedArtifact(artifactResolutionHelper.resolveArtifact(artifact));
            }
        }
    }

    private String determineVersion(ArtifactConfiguration artifact) throws MojoExecutionException {
        Optional<Artifact> resolvedDependency = project.getArtifacts()
                .stream()
                .filter(a -> {
                    return Objects.equals(a.getGroupId(), artifact.getGroupId()) &&
                            Objects.equals(a.getArtifactId(), artifact.getArtifactId()) &&
                            Objects.equals(a.getClassifier(), artifact.getClassifier()) &&
                            Objects.equals(a.getType(), artifact.getType());
                })
                .findFirst();

        if (resolvedDependency.isPresent()) {
            return resolvedDependency.get().getVersion();
        }

        if (project.getDependencyManagement() != null) {
            Optional<org.apache.maven.model.Dependency> managed = project.getDependencyManagement()
                    .getDependencies()
                    .stream()
                    .filter(d -> {
                        return Objects.equals(d.getGroupId(), artifact.getGroupId()) &&
                                Objects.equals(d.getArtifactId(), artifact.getArtifactId()) &&
                                Objects.equals(d.getClassifier(), artifact.getClassifier()) &&
                                Objects.equals(d.getType(), artifact.getType());
                    })
                    .findFirst();

            if (managed.isPresent()) {
                return managed.get().getVersion();
            }
        }

        throw new MojoExecutionException(
                "A version must be given for artifact " + artifact.toDependencyString() +
                        ". Either specify one explicitly, add it to the project dependencies" +
                        " or add it to the project's dependency management.");
    }

    private String getVersion(ModuleConfiguration moduleConfiguration) {
        if (moduleConfiguration.getVersion() != null) {
            return moduleConfiguration.getVersion();
        }
        else if (moduleConfiguration.getArtifact() != null) {
            return moduleConfiguration.getArtifact().getVersion();
        }
        // TODO try to derive version from file name?
        else if (moduleConfiguration.getFile() != null) {
            return null;
        }
        else {
            return null;
        }
    }

    private Path getInputFile(ModuleConfiguration moduleConfiguration, ArtifactResolutionHelper artifactResolutionHelper) throws MojoExecutionException {
        if (moduleConfiguration.getFile() != null) {
            if (moduleConfiguration.getArtifact() != null) {
                throw new MojoExecutionException("Only one of 'file' and 'artifact' may be specified, but both are given for"
                        + moduleConfiguration.getArtifact().toDependencyString());
            }
            else {
                return moduleConfiguration.getFile().toPath();
            }
        }
        else if (moduleConfiguration.getArtifact() != null) {
            return moduleConfiguration.getResolvedArtifact().getFile().toPath();
        }
        else {
            throw new MojoExecutionException("One of 'file' and 'artifact' must be specified");
        }
    }

    private String getModuleInfoSource(Path inputFile, ModuleConfiguration moduleConfiguration, ModuleInfoGenerator moduleInfoGenerator,
                                       Map<ArtifactIdentifier, String> assignedNamesByModule, Map<ArtifactIdentifier, Path> modularizedJars)
            throws MojoExecutionException {
        if (moduleConfiguration.getModuleInfo() != null && moduleConfiguration.getModuleInfoSource() == null && moduleConfiguration.getModuleInfoFile() == null) {
            GeneratedModuleInfo generatedModuleInfo;

            if (moduleConfiguration.getArtifact() != null) {
                generatedModuleInfo = moduleInfoGenerator.generateModuleInfo(
                        moduleConfiguration.getArtifact(),
                        moduleConfiguration.getAdditionalDependencies(),
                        moduleConfiguration.getModuleInfo(),
                        assignedNamesByModule,
                        modularizedJars);
            }
            else {
                generatedModuleInfo = moduleInfoGenerator.generateModuleInfo(
                        moduleConfiguration.getFile().toPath(),
                        moduleConfiguration.getAdditionalDependencies(),
                        moduleConfiguration.getModuleInfo(),
                        assignedNamesByModule);
            }

            return getLines(generatedModuleInfo.getPath());
        }
        else if (moduleConfiguration.getModuleInfo() == null && moduleConfiguration.getModuleInfoSource() != null && moduleConfiguration.getModuleInfoFile() == null) {
            return moduleConfiguration.getModuleInfoSource();
        }
        else if (moduleConfiguration.getModuleInfo() == null && moduleConfiguration.getModuleInfoSource() == null && moduleConfiguration.getModuleInfoFile() != null) {
            return getLines(moduleConfiguration.getModuleInfoFile().toPath());
        }
        else {
            throw new MojoExecutionException("Either 'moduleInfo' or 'moduleInfoFile' or 'moduleInfoSource' must be specified for " + inputFile);
        }
    }

    private String getModuleInfoSource(Path inputFile, MainModuleConfiguration moduleConfiguration, ModuleInfoGenerator moduleInfoGenerator,
                                       Map<ArtifactIdentifier, String> assignedNamesByModule, Map<ArtifactIdentifier, Path> modularizedJars)
            throws MojoExecutionException {
        if (moduleConfiguration.getModuleInfo() != null && moduleConfiguration.getModuleInfoSource() == null && moduleConfiguration.getModuleInfoFile() == null) {

            Set<DependencyDescriptor> dependencies = project.getArtifacts().stream()
                    .map(d -> new DependencyDescriptor(
                            d.getFile().toPath(),
                            d.isOptional(),
                            getAssignedModuleName(assignedNamesByModule, d)))
                    .collect(Collectors.toSet());

            GeneratedModuleInfo generatedModuleInfo = moduleInfoGenerator.generateModuleInfo(
                    inputFile,
                    dependencies,
                    moduleConfiguration.getModuleInfo());

            return getLines(generatedModuleInfo.getPath());
        }
        else if (moduleConfiguration.getModuleInfo() == null && moduleConfiguration.getModuleInfoSource() != null && moduleConfiguration.getModuleInfoFile() == null) {
            return moduleConfiguration.getModuleInfoSource();
        }
        else if (moduleConfiguration.getModuleInfo() == null && moduleConfiguration.getModuleInfoSource() == null && moduleConfiguration.getModuleInfoFile() != null) {
            return getLines(moduleConfiguration.getModuleInfoFile().toPath());
        }
        else {
            throw new MojoExecutionException("Either 'moduleInfo' or 'moduleInfoFile' or 'moduleInfoSource' must be specified for <module>.");
        }
    }

    private String getLines(Path file) throws MojoExecutionException {
        try {
            return new String(Files.readAllBytes(file));
        }
        catch (IOException e) {
            throw new MojoExecutionException("Couldn't read file " + file);
        }
    }

    private void createDirectories() {
        if (!workingDirectory.exists()) {
            workingDirectory.mkdirs();
        }

        File internalGeneratedSourcesDir = new File(workingDirectory, "generated-sources");
        if (!internalGeneratedSourcesDir.exists()) {
            internalGeneratedSourcesDir.mkdirs();
        }

        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }
    }

    private Map<ArtifactIdentifier, String> getAssignedModuleNamesByModule(ArtifactResolutionHelper artifactResolutionHelper) throws MojoExecutionException {
        Map<ArtifactIdentifier, String> assignedNamesByModule = new HashMap<>();

        if (modules == null) {
            return assignedNamesByModule;
        }

        for (ModuleConfiguration configuredModule : modules) {
            String assignedName;

            if (configuredModule.getModuleInfo() != null) {
                assignedName = configuredModule.getModuleInfo().getName();
            }
            else if (configuredModule.getModuleInfoFile() != null) {
                assignedName = ModuleInfoCompiler.parseModuleInfo(configuredModule.getModuleInfoFile().toPath()).getNameAsString();
            }
            else {
                assignedName = ModuleInfoCompiler.parseModuleInfo(configuredModule.getModuleInfoSource()).getNameAsString();
            }

            // TODO handle file case; although file is unlikely to be used together with others
            if (configuredModule.getArtifact() != null) {
                assignedNamesByModule.put(
                        new ArtifactIdentifier(configuredModule.getResolvedArtifact()),
                        assignedName);
            }
        }

        return assignedNamesByModule;
    }

    private String getAssignedModuleName(Map<ArtifactIdentifier, String> assignedNamesByModule, Artifact artifact) {
        for (Entry<ArtifactIdentifier, String> assignedNameByModule : assignedNamesByModule.entrySet()) {
            // ignoring the version; the resolved artifact could have a different version then the one used
            // in this modularization build
            if (assignedNameByModule.getKey().getGroupId().equals(artifact.getGroupId()) &&
                    assignedNameByModule.getKey().getArtifactId().equals(artifact.getArtifactId()) &&
                    (artifact.getClassifier() == null && assignedNameByModule.getKey().getClassifier().equals("")
                            || assignedNameByModule.getKey().getClassifier().equals(artifact.getClassifier()))
                    &&
                    assignedNameByModule.getKey().getExtension().equals(artifact.getType())) {
                return assignedNameByModule.getValue();
            }
        }

        return null;
    }

    private static class InstantConverter {

        private static final Instant DATE_MIN = Instant.parse("1980-01-01T00:00:02Z");
        private static final Instant DATE_MAX = Instant.parse("2099-12-31T23:59:59Z");

        public static Instant convert(String value) {
            if (value == null) {
                return null;
            }

            // Number representing seconds since the epoch
            if (!value.isEmpty() && isNumeric(value)) {
                return Instant.ofEpochSecond(Long.parseLong(value.trim()));
            }

            try {
                // Parse the date in UTC such as '2011-12-03T10:15:30Z' or with an offset '2019-10-05T20:37:42+06:00'.
                final Instant date = OffsetDateTime.parse(value)
                        .withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS).toInstant();

                if (date.isBefore(DATE_MIN) || date.isAfter(DATE_MAX)) {
                    throw new IllegalArgumentException("'" + date + "' is not within the valid range "
                            + DATE_MIN + " to " + DATE_MAX);
                }
                return date;
            }
            catch (DateTimeParseException pe) {
                throw new IllegalArgumentException("Invalid project.build.outputTimestamp value '" + value + "'",
                        pe);
            }
        }

        private static boolean isNumeric(String str) {
            try {
                Long.parseLong(str.trim());
                return true;
            }
            catch (NumberFormatException e) {
                return false;
            }
        }
    }
}
