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
package org.moditect.generator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.DependencyNode;
import org.moditect.commands.AddModuleInfo;
import org.moditect.commands.GenerateModuleInfo;
import org.moditect.dependency.ArtifactResolver;
import org.moditect.internal.compiler.ModuleInfoCompiler;
import org.moditect.model.add.AddModuleConfiguration;
import org.moditect.model.add.MainModuleConfiguration;
import org.moditect.model.common.ArtifactConfiguration;
import org.moditect.model.common.ModuleInfoConfiguration;
import org.moditect.model.generate.ArtifactIdentifier;
import org.moditect.model.DependencePattern;
import org.moditect.model.DependencyDescriptor;
import org.moditect.model.GeneratedModuleInfo;
import org.moditect.model.PackageNamePattern;
import org.moditect.model.generate.ModuleConfiguration;
import org.moditect.spi.log.Log;

/**
 * Generate a module-info.class descriptor.
 *
 * @author Gunnar Morling
 * @author Pratik Parikh
 */
public class ModuleInfoGenerator {

    private final ArtifactResolver artifactResolver;
    private final ArtifactResolutionHelper artifactResolutionHelper;
    private final List<String> jdepsExtraArgs;
    private final Log log;
    private final File workingDirectory;
    private final File outputDirectory;
    private List<? extends ModuleConfiguration> modules;
    private String artifactOverride;
    private String moduleNameOverride;
    private String additionalDependenciesOverride;
    private String exportExcludesOverride;
    private boolean addServiceUsesOverride;

    public ModuleInfoGenerator(ArtifactResolver artifactResolver,  ArtifactResolutionHelper artifactResolutionHelper,
                               List<String> jdepsExtraArgs, Log log, File workingDirectory, File outputDirectory,
                               List<? extends ModuleConfiguration> modules,String artifactOverride,String moduleNameOverride,
                               String additionalDependenciesOverride,String exportExcludesOverride,
                               boolean addServiceUsesOverride) {
        this.artifactResolver = artifactResolver;
        this.artifactResolutionHelper = artifactResolutionHelper;
        this.jdepsExtraArgs = jdepsExtraArgs;
        this.log = log;
        this.workingDirectory = workingDirectory;
        this.outputDirectory = outputDirectory;
        this.modules=modules;
        this.artifactOverride=artifactOverride;
        this.moduleNameOverride=moduleNameOverride;
        this.additionalDependenciesOverride = additionalDependenciesOverride;
        this.exportExcludesOverride=exportExcludesOverride;
        this.addServiceUsesOverride=addServiceUsesOverride;
    }

    public ModuleInfoGenerator(ArtifactResolver artifactResolver,  ArtifactResolutionHelper artifactResolutionHelper,
                               List<String> jdepsExtraArgs, Log log, File workingDirectory, File outputDirectory,
                               List<? extends ModuleConfiguration> modules) {
        this(artifactResolver,artifactResolutionHelper,jdepsExtraArgs,log,workingDirectory,outputDirectory,modules,null
        , null, null, null, false);
    }

    public void add(File generatedSourceDirectory,boolean overwriteExistingFiles,MainModuleConfiguration module
            ,File buildDirectory, String artifactId, String version) throws Exception {
        createDirectories(generatedSourceDirectory);
        final Path outputPath = outputDirectory.toPath();
        resolveArtifactsToBeModularized( artifactResolutionHelper );

        final Map<ArtifactIdentifier, String> assignedNamesByModule =
                getAddAssignedModuleNamesByModule( artifactResolutionHelper );
        Map<ArtifactIdentifier, Path> modularizedJars = new HashMap<>();

        if ( modules != null ) {
            for ( ModuleConfiguration moduleConfiguration : modules ) {
                final Path inputFile = getInputFile( (AddModuleConfiguration) moduleConfiguration, artifactResolutionHelper );
                final String moduleInfoSource = getAddModuleInfoSource( inputFile, (AddModuleConfiguration) moduleConfiguration
                        , this, assignedNamesByModule, modularizedJars );

                final AddModuleInfo addModuleInfo = new AddModuleInfo( moduleInfoSource,
                        ((AddModuleConfiguration)moduleConfiguration).getMainClass(),
                        getVersion( ((AddModuleConfiguration) moduleConfiguration) ),
                        inputFile, outputPath, overwriteExistingFiles, module, assignedNamesByModule, modularizedJars,
                        this);

                addModuleInfo.run();

                if ( moduleConfiguration.getArtifact() != null ) {
                    modularizedJars.put(
                            new ArtifactIdentifier( ((AddModuleConfiguration) moduleConfiguration).getResolvedArtifact() ),
                            outputPath.resolve( inputFile.getFileName() )
                    );
                }
            }
        }

        if ( module != null ) {
            final Path inputJar = buildDirectory.toPath().resolve(artifactId + "-" + version + ".jar");
            final AddModuleInfo addModuleInfo = new AddModuleInfo(null, module.getMainClass(),
                    version, inputJar, outputPath, overwriteExistingFiles, module, assignedNamesByModule,
                    modularizedJars,this);
            if (!Files.exists(inputJar)) {
               // prepare for next task, this only apply for gradle as maven is sequential
                artifactResolver.postActivity(addModuleInfo,module,this,
                        assignedNamesByModule, modularizedJars);
            } else {
                addModuleInfo.initalizeModuleInfoSource().run();
                try {
                    Files.copy(outputPath.resolve(inputJar.getFileName()), inputJar, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new RuntimeException("Couldn't replace " + inputJar + " with modularized version", e);
                }
            }
        }
    }

    /**
     * Sets the resolved artifact of all artifact configurations. If no version is given, we try
     * to obtain it from the project dependencies.
     */
    private void resolveArtifactsToBeModularized(ArtifactResolutionHelper artifactResolutionHelper)
            throws Exception {
        if ( modules == null ) {
            return;
        }

        for ( ModuleConfiguration moduleConfiguration : modules ) {
            ArtifactConfiguration artifact = moduleConfiguration.getArtifact();

            if ( artifact != null ) {
                if ( artifact.getVersion() == null ) {
                    artifact.setVersion( artifactResolver.determineVersion( artifact ) );
                }

                ((AddModuleConfiguration)moduleConfiguration).setResolvedArtifact(
                        artifactResolutionHelper.resolveArtifact( artifact ) );
            }
        }
    }

    private String getVersion(AddModuleConfiguration moduleConfiguration) {
        if ( moduleConfiguration.getVersion() != null ) {
            return moduleConfiguration.getVersion();
        }
        else if ( moduleConfiguration.getArtifact() != null ) {
            return moduleConfiguration.getArtifact().getVersion();
        }
        // TODO try to derive version from file name?
        else if ( moduleConfiguration.getFile() != null ) {
            return null;
        }
        else {
            return null;
        }
    }

    private Path getInputFile(AddModuleConfiguration moduleConfiguration,
                              ArtifactResolutionHelper artifactResolutionHelper) throws Exception {
        if ( moduleConfiguration.getFile() != null ) {
            if ( moduleConfiguration.getArtifact() != null ) {
                throw new Exception( "Only one of 'file' and 'artifact' may be specified, but both are given for"
                        + moduleConfiguration.getArtifact().toDependencyString() );
            }
            else {
                return moduleConfiguration.getFile().toPath();
            }
        }
        else if ( moduleConfiguration.getArtifact() != null ) {
            return moduleConfiguration.getResolvedArtifact().getFile().toPath();
        }
        else {
            throw new Exception( "One of 'file' and 'artifact' must be specified" );
        }
    }

    private String getAddModuleInfoSource(Path inputFile, AddModuleConfiguration moduleConfiguration,
                                       ModuleInfoGenerator moduleInfoGenerator,
                                       Map<ArtifactIdentifier, String> assignedNamesByModule,
                                       Map<ArtifactIdentifier, Path> modularizedJars) throws Exception {
        if ( moduleConfiguration.getModuleInfo() != null
                && moduleConfiguration.getModuleInfoSource() == null
                && moduleConfiguration.getModuleInfoFile() == null ) {
            GeneratedModuleInfo generatedModuleInfo = null;
            try{
                if ( moduleConfiguration.getArtifact() != null ) {
                    generatedModuleInfo = moduleInfoGenerator.generateModuleInfo(
                            moduleConfiguration.getArtifact(),
                            moduleConfiguration.getAdditionalDependencies(),
                            moduleConfiguration.getModuleInfo(),
                            assignedNamesByModule,
                            modularizedJars,
                            moduleConfiguration.getIncludeOptional(),
                            moduleConfiguration.getExcludeDependencies()
                    );
                }
                else {
                    generatedModuleInfo = moduleInfoGenerator.generateModuleInfo(
                            moduleConfiguration.getFile().toPath(),
                            moduleConfiguration.getAdditionalDependencies(),
                            moduleConfiguration.getModuleInfo(),
                            assignedNamesByModule
                    );
                }
            } catch (Exception e) {
                throw new Exception("Problem generating module configuration",e);
            }

            return getLines( generatedModuleInfo.getPath() );
        }
        else if ( moduleConfiguration.getModuleInfo() == null
                && moduleConfiguration.getModuleInfoSource() != null
                && moduleConfiguration.getModuleInfoFile() == null ) {
            return moduleConfiguration.getModuleInfoSource();
        }
        else if ( moduleConfiguration.getModuleInfo() != null
                && moduleConfiguration.getModuleInfoSource() != null
                && moduleConfiguration.getModuleInfoFile() == null ) {
            return moduleConfiguration.getModuleInfoSource();
        }
        else if ( moduleConfiguration.getModuleInfo() == null
                && moduleConfiguration.getModuleInfoSource() == null
                && moduleConfiguration.getModuleInfoFile() != null ) {
            return getLines( moduleConfiguration.getModuleInfoFile().toPath() );
        }
        else {
            throw new Exception( "Either 'moduleInfo' or 'moduleInfoFile' or 'moduleInfoSource' must be specified for "
                    + inputFile);
        }
    }

    public String getModuleInfoSource(Path inputFile, MainModuleConfiguration moduleConfiguration,
                                       Map<ArtifactIdentifier, String> assignedNamesByModule,
                                       Map<ArtifactIdentifier, Path> modularizedJars) throws Exception {
        if ( moduleConfiguration.getModuleInfo() != null && moduleConfiguration.getModuleInfoSource() == null
                && moduleConfiguration.getModuleInfoFile() == null ) {

            final Set<DependencyDescriptor> dependencies =
                    artifactResolver.getDependencyDescriptors(assignedNamesByModule);

            GeneratedModuleInfo generatedModuleInfo = null;
            try {
                generatedModuleInfo = generateModuleInfo(
                        inputFile,
                        dependencies,
                        moduleConfiguration.getModuleInfo()
                );
            } catch (Exception e) {
                throw new Exception("Problem generating module configuration",e);
            }

            return getLines( generatedModuleInfo.getPath() );
        }
        else if ( moduleConfiguration.getModuleInfo() == null && moduleConfiguration.getModuleInfoSource() != null && moduleConfiguration.getModuleInfoFile() == null ) {
            return moduleConfiguration.getModuleInfoSource();
        }
        else if ( moduleConfiguration.getModuleInfo() == null && moduleConfiguration.getModuleInfoSource() == null && moduleConfiguration.getModuleInfoFile() != null ) {
            return getLines( moduleConfiguration.getModuleInfoFile().toPath() );
        }
        else {
            throw new Exception(
                    "Either 'moduleInfo' or 'moduleInfoFile' or 'moduleInfoSource' must be specified for <module>." );
        }
    }

    private String getLines(Path file) throws Exception {
        try {
            return new String(Files.readAllBytes( file ) );
        }
        catch (IOException e) {
            throw new Exception( "Couldn't read file " + file );
        }
    }

    private Map<ArtifactIdentifier, String> getAddAssignedModuleNamesByModule(
            ArtifactResolutionHelper artifactResolutionHelper) throws Exception {
        Map<ArtifactIdentifier, String> assignedNamesByModule = new HashMap<>();

        if ( modules == null ) {
            return assignedNamesByModule;
        }

        for ( ModuleConfiguration configuredModule : modules ) {
            String assignedName;

            if ( configuredModule.getModuleInfo() != null ) {
                assignedName = configuredModule.getModuleInfo().getName();
            }
            else if ( ((AddModuleConfiguration)configuredModule).getModuleInfoFile() != null ) {
                assignedName = ModuleInfoCompiler.parseModuleInfo(
                        ((AddModuleConfiguration)configuredModule).getModuleInfoFile().toPath() ).getNameAsString();
            }
            else {
                assignedName = ModuleInfoCompiler.parseModuleInfo(
                        ((AddModuleConfiguration)configuredModule).getModuleInfoSource() ).getNameAsString();
            }

            // TODO handle file case; although file is unlikely to be used together with others
            if ( configuredModule.getArtifact() != null ) {
                assignedNamesByModule.put(
                        new ArtifactIdentifier( ((AddModuleConfiguration)configuredModule).getResolvedArtifact() ),
                        assignedName
                );
            }
        }

        return assignedNamesByModule;
    }

    public void generate() throws Exception {
        createDirectories(null);
        final Map<ArtifactIdentifier, String> assignedNamesByModule = getAssignedModuleNamesByModule(
                artifactResolutionHelper );
        if ( artifactOverride != null && !artifactOverride.isEmpty()) {
            final ModuleConfiguration moduleConfiguration = getModuleConfigurationFromOverrides();
            generateModuleInfo(
                    moduleConfiguration.getArtifact(),
                    moduleConfiguration.getAdditionalDependencies(),
                    moduleConfiguration.getModuleInfo(),
                    assignedNamesByModule,
                    Collections.emptyMap(),
                    moduleConfiguration.getIncludeOptional(),
                    moduleConfiguration.getExcludeDependencies()
            );
        }
        else {
            for ( ModuleConfiguration moduleConfiguration : modules ) {
                generateModuleInfo(
                        moduleConfiguration.getArtifact(),
                        moduleConfiguration.getAdditionalDependencies(),
                        moduleConfiguration.getModuleInfo(),
                        assignedNamesByModule,
                        Collections.emptyMap(),
                        moduleConfiguration.getIncludeOptional(),
                        moduleConfiguration.getExcludeDependencies()
                );
            }
        }
    }

    public GeneratedModuleInfo generateModuleInfo(ArtifactConfiguration artifact,
                                                  List<ArtifactConfiguration> additionalDependencies,
                                                  ModuleInfoConfiguration moduleInfo,
                                                  Map<ArtifactIdentifier, String> assignedNamesByModule,
                                                  Map<ArtifactIdentifier, Path> modularizedJars,
                                                  boolean includeOptional,
                                                  List<ArtifactConfiguration> excludeDependencies) throws Exception {
        final Artifact inputArtifact = artifactResolutionHelper.resolveArtifact(artifact);
        final Set<DependencyDescriptor> dependencies = getDependencies( inputArtifact, assignedNamesByModule,
                modularizedJars ,includeOptional,excludeDependencies);

        for( ArtifactConfiguration further : additionalDependencies ) {
            final Artifact furtherArtifact = artifactResolutionHelper.resolveArtifact( further );
            final Path modularized = getModularizedJar( modularizedJars, new ArtifactIdentifier( further.getGroupId(),
                    further.getArtifactId(), further.getVersion(), further.getType(), further.getClassifier() ) );
            dependencies.add(
                    new DependencyDescriptor( modularized != null ? modularized : furtherArtifact.getFile().toPath(),
                            false, null ) );
        }

        return generateModuleInfo( inputArtifact.getFile().toPath(), dependencies, moduleInfo);
    }

    public GeneratedModuleInfo generateModuleInfo(Path inputJar, List<ArtifactConfiguration> additionalDependencies,
                                                  ModuleInfoConfiguration moduleInfo,
                                                  Map<ArtifactIdentifier, String> assignedNamesByModule) throws Exception {
        Set<DependencyDescriptor> dependencies = new HashSet<>();

        for( ArtifactConfiguration further : additionalDependencies ) {
            final Artifact furtherArtifact = artifactResolutionHelper.resolveArtifact( further );
            dependencies.add( new DependencyDescriptor( furtherArtifact.getFile().toPath(), false,
                    null ) );
        }

        return generateModuleInfo( inputJar, dependencies, moduleInfo);
    }

    public GeneratedModuleInfo generateModuleInfo(Path inputJar, Set<DependencyDescriptor> dependencies,
                                                  ModuleInfoConfiguration moduleInfo) throws Exception {
        Set<String> uses;

        if ( moduleInfo.getUses() != null ) {
            uses = Arrays.stream( moduleInfo.getUses().split( ";" ) )
                .map( String::trim )
                .collect( Collectors.toSet() );
        }
        else {
            uses = Collections.emptySet();
        }
        return new GenerateModuleInfo(
                inputJar,
                moduleInfo.getName(),
                moduleInfo.isOpen(),
                dependencies,
                PackageNamePattern.parsePatterns( moduleInfo.getExports() ),
                PackageNamePattern.parsePatterns( moduleInfo.getOpens() ),
                DependencePattern.parsePatterns( moduleInfo.getRequires() ),
                workingDirectory.toPath(),
                outputDirectory.toPath(),
                uses,
                moduleInfo.isAddServiceUses(),
                jdepsExtraArgs,
                log
        )
        .run();
    }

    private Set<DependencyDescriptor> getDependencies(Artifact inputArtifact,
                                                      Map<ArtifactIdentifier, String> assignedNamesByModule,
                                                      Map<ArtifactIdentifier, Path> modularizedJars,
                                                      boolean includeOptional,
                                                      List<ArtifactConfiguration> excludeDependencies) throws Exception {
        Set<DependencyDescriptor> dependencies = new LinkedHashSet<>();

        final List<DependencyNode> compilationDependencies =
                artifactResolutionHelper.getCompilationDependencies(inputArtifact, artifactResolver);

        for ( final DependencyNode dependency : compilationDependencies) {
            if((!includeOptional && dependency.getDependency().isOptional()) ||
                    excludeDependencies.stream().filter(excludeDependency -> {
                        return excludeDependency.getArtifactId() != null
                                && excludeDependency.getArtifactId().equals(
                                        dependency.getDependency().getArtifact().getArtifactId())
                                && excludeDependency.getGroupId() != null
                                && excludeDependency.getGroupId().equals(
                                        dependency.getDependency().getArtifact().getGroupId());
                    }).findFirst().isPresent()){
                continue;
            }

            Artifact artifact = dependency.getDependency().getArtifact();
            // use the version of the dependency as used within the current project's build, if present
            final String versionFromProject = getVersionFromProject( artifact );
            if ( versionFromProject != null ) {
                artifact = new DefaultArtifact( artifact.getGroupId(), artifact.getArtifactId(),
                        "", artifact.getExtension(), versionFromProject );
            }

            final Artifact resolvedDependency = artifactResolutionHelper.resolveArtifact( artifact );
            final String assignedModuleName = getAssignedModuleName( assignedNamesByModule,
                    new ArtifactIdentifier( resolvedDependency ) );
            final Path modularized = getModularizedJar( modularizedJars, new ArtifactIdentifier( resolvedDependency ) );
            final DependencyDescriptor dependencyDescriptor = new DependencyDescriptor(
                    modularized != null ? modularized : resolvedDependency.getFile().toPath(),
                    dependency.getDependency().isOptional(),
                    assignedModuleName
            );
            // only add if dependency is not already added.
            if(!dependencies.contains(dependencyDescriptor)) {

                dependencies.add(
                        dependencyDescriptor
                );
            }
        }

        return dependencies;
    }

    private String getAssignedModuleName(Map<ArtifactIdentifier, String> assignedNamesByModule,
                                         ArtifactIdentifier artifactIdentifier) {
        for ( Entry<ArtifactIdentifier, String> assignedNameByModule : assignedNamesByModule.entrySet() ) {
            // ignoring the version; the resolved artifact could have a different version then the one used
            // in this modularization build
            if ( assignedNameByModule.getKey().getGroupId().equals( artifactIdentifier.getGroupId() ) &&
                    assignedNameByModule.getKey().getArtifactId().equals( artifactIdentifier.getArtifactId() ) &&
                    assignedNameByModule.getKey().getClassifier().equals( artifactIdentifier.getClassifier() ) &&
                    assignedNameByModule.getKey().getExtension().equals( artifactIdentifier.getExtension() ) ) {
                return assignedNameByModule.getValue();
            }
        }

        return null;
    }

    private Path getModularizedJar(Map<ArtifactIdentifier, Path> modularizedJars, ArtifactIdentifier artifactIdentifier) {
        for ( Entry<ArtifactIdentifier, Path> assignedNameByModule : modularizedJars.entrySet() ) {
            // ignoring the version; the resolved artifact could have a different version than the one used
            // in this modularization build
            if ( assignedNameByModule.getKey().getGroupId().equals( artifactIdentifier.getGroupId() ) &&
                    assignedNameByModule.getKey().getArtifactId().equals( artifactIdentifier.getArtifactId() ) &&
                    areEqualClassifiers( assignedNameByModule.getKey().getClassifier(), artifactIdentifier.getClassifier() ) &&
                    assignedNameByModule.getKey().getExtension().equals( artifactIdentifier.getExtension() ) ) {
                return assignedNameByModule.getValue();
            }
        }

        return null;
    }

    private String getVersionFromProject(Artifact artifact) throws Exception {
        return artifactResolver.getVersionFromProject(artifact);
    }

    private Map<ArtifactIdentifier, String> getAssignedModuleNamesByModule(
            ArtifactResolutionHelper artifactResolutionHelper) throws Exception {
        Map<ArtifactIdentifier, String> assignedNamesByModule = new HashMap<>();

        for ( ModuleConfiguration configuredModule : modules ) {
            assignedNamesByModule.put(
                    new ArtifactIdentifier( artifactResolutionHelper.resolveArtifact( configuredModule.getArtifact() ) ),
                    configuredModule.getModuleInfo().getName()
            );
        }

        return assignedNamesByModule;
    }

    private ModuleConfiguration getModuleConfigurationFromOverrides() {
        ModuleConfiguration moduleConfiguration = new ModuleConfiguration();

        moduleConfiguration.setArtifact( new ArtifactConfiguration( artifactOverride ) );
        moduleConfiguration.setModuleInfo( new ModuleInfoConfiguration() );
        moduleConfiguration.getModuleInfo().setName( moduleNameOverride );

        if ( additionalDependenciesOverride != null ) {
            for ( String additionalDependency : additionalDependenciesOverride.split( "\\," ) ) {
                moduleConfiguration.getAdditionalDependencies().add( new ArtifactConfiguration( additionalDependency ) );
            }
        }

        if ( exportExcludesOverride != null ) {
            moduleConfiguration.getModuleInfo().setExports( exportExcludesOverride );
        }

        moduleConfiguration.getModuleInfo().setAddServiceUses( addServiceUsesOverride );

        return moduleConfiguration;
    }

    private void createDirectories(File generatedSourceDirectory) {
        if ( !workingDirectory.exists() ) {
            workingDirectory.mkdirs();
        }

        if(generatedSourceDirectory != null && !generatedSourceDirectory.exists()){
            generatedSourceDirectory.mkdirs();
        }

        if ( !outputDirectory.exists() ) {
            outputDirectory.mkdirs();
        }
    }

    public static boolean areEqualClassifiers(String classifier1, String classifier2) {
        if ( classifier1 != null && classifier1.isEmpty() ) {
            classifier1 = null;
        }
        if ( classifier2 != null && classifier2.isEmpty() ) {
            classifier2 = null;
        }

        return Objects.equals( classifier1, classifier2 );
    }
}
