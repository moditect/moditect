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
package org.moditect.gradleplugin.task;

import groovy.lang.Closure;
import org.gradle.api.Project;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.moditect.model.add.AddModuleConfiguration;
import org.moditect.model.add.MainModuleConfiguration;
import org.moditect.model.common.ArtifactConfiguration;
import org.moditect.model.common.ModuleInfoConfiguration;
import org.moditect.model.generate.ModuleConfiguration;
import org.moditect.model.image.Launcher;

import java.io.File;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Gradle Extension for moditect configuration in the buildfile (build.gradle).
 *
 * @author Pratik Parikh
 */
public class ModitectExtension {

    final AddModuleInfo moduleInfo;

    final GenerateModuleInfo generateModuleInfo;

    final Project project;

    final ApplicationImage image;

    public ModitectExtension(Project project) {
        this.project = project;
        this.moduleInfo = new AddModuleInfo(project);
        final Path buildDirectoryPath = project.getBuildDir().toPath();
        final File moditectDirectoryFile = buildDirectoryPath.resolve("moditect").toFile();
        this.moduleInfo.setArtifactId(project.getName());
        this.moduleInfo.setBuildDirectory(buildDirectoryPath.resolve("libs").toFile());
        this.moduleInfo.setWorkingDirectory(moditectDirectoryFile);
        this.moduleInfo.setOutputDirectory(buildDirectoryPath.resolve("modules").toFile());
        image = new ApplicationImage(project);
        image.setOutputDirectory(
                buildDirectoryPath.resolve("image").toFile().toPath());
        generateModuleInfo = new GenerateModuleInfo(project);
        generateModuleInfo.setWorkingDirectory(moditectDirectoryFile);
        generateModuleInfo.setOutputDirectory(
                buildDirectoryPath.resolve("generated-sources").resolve("modules").toFile());
    }

    @Input
    public AddModuleInfo getModuleInfo() {
        return moduleInfo;
    }

    public void moduleInfo(Closure closure) {
        project.configure(moduleInfo,closure);
    }

    @Input
    public ApplicationImage getImage() {
        return image;
    }

    public void image(Closure closure) {
        project.configure(image,closure);
    }

    @Input
    public GenerateModuleInfo getGenerateModuleInfo() {
        return generateModuleInfo;
    }

    public void generateModuleInfo(Closure closure) {
        project.configure(generateModuleInfo,closure);
    }

    @Override
    public String toString() {
        return "ModitectExtension{" +
                " moduleInfo=" + moduleInfo +
                ", generateModuleInfo=" + generateModuleInfo +
                ", project=" + project +
                '}';
    }

    public static class ApplicationImage{

        private Set<Path> modulePath = new TreeSet<>();
        private List<String> modules = new LinkedList<>();
        private List<String> excludeResources = new LinkedList<>();
        private Launcher launcher = new Launcher();
        private Path outputDirectory;
        private Integer compression;
        private boolean stripDebug;

        @Internal
        final Project project;

        public ApplicationImage(Project project){
            this.stripDebug = false;
            this.project=project;
        }

        @Input
        public Set<Path> getModulePath(){
            return this.modulePath;
        }

        public void setModulePath(final Set<Path> modulePath){
            this.modulePath = modulePath;
        }

        @Input
        public List<String> getModules(){
            return this.modules;
        }

        public void setModules(List<String> modules){
            this.modules = modules;
        }

        @Input
        public Path getOutputDirectory(){
            return this.outputDirectory;
        }

        public void setOutputDirectory(final Path outputDirectory){
            this.outputDirectory=outputDirectory;
        }

        @Input
        public Launcher getLauncher(){
            return this.launcher;
        }

        public void launcher(Closure closure) {
            project.configure(launcher,closure);
        }

        @Input
        public Integer getCompression(){
            return this.compression;
        }

        public void setCompression(final Integer compression){
            this.compression=compression;
        }

        @Input
        public boolean getStripDebug(){
            return this.stripDebug;
        }

        public void setStripDebug(final boolean stripDebug){
            this.stripDebug=stripDebug;
        }

        @Input
        public List<String> getExcludeResources(){
            return this.excludeResources;
        }

        public void setExcludeResources(List<String> excludeResources){
            this.excludeResources = excludeResources;
        }

        @Override
        public String toString() {
            return "ApplicationImage{" +
                    "modulePath=" + modulePath +
                    ", modules=" + modules +
                    ", outputDirectory=" + outputDirectory +
                    ", launcher=" + launcher +
                    ", compression=" + compression +
                    ", stripDebug=" + stripDebug +
                    ", excludeResources=" + excludeResources +
                    '}';
        }
    }

    public static class GenerateModuleInfo{
        private File workingDirectory;

        private File outputDirectory;

        private List<Module> modules = new LinkedList<>();

        private List<String> jdepsExtraArgs = new LinkedList<>();

        private String artifact;

        private String additionalDependencies;

        private String moduleName;

        private String exportExcludes;

        private boolean addServiceUses;

        @Internal
        final Project project;

        public GenerateModuleInfo(final Project project){
            this.addServiceUses = false;
            this.project = project;
        }

        @Internal
        public File getWorkingDirectory() {
            return workingDirectory;
        }

        public void setWorkingDirectory(File workingDirectory) {
            this.workingDirectory = workingDirectory;
        }

        @Input
        public File getOutputDirectory() {
            return outputDirectory;
        }

        public void setOutputDirectory(File outputDirectory) {
            this.outputDirectory = outputDirectory;
        }

        @Internal
        public List<Module> getModules() {
            return modules;
        }

        public void module(Closure closure){
            final Module module = new Module(project);
            project.configure(module,closure);
            modules.add(module);
        }

        @Input
        public List<String> getJdepsExtraArgs() {
            return jdepsExtraArgs;
        }

        public void setJdepsExtraArgs(List<String> jdepsExtraArgs) {
            this.jdepsExtraArgs = jdepsExtraArgs;
        }

        @Input
        public String getArtifact() {
            return artifact;
        }

        public void setArtifact(String artifact) {
            this.artifact = artifact;
        }

        @Input
        public String getAdditionalDependencies() {
            return additionalDependencies;
        }

        public void setAdditionalDependencies(String additionalDependencies) {
            this.additionalDependencies = additionalDependencies;
        }

        @Input
        public String getModuleName() {
            return moduleName;
        }

        public void setModuleName(String moduleName) {
            this.moduleName = moduleName;
        }

        @Input
        public String getExportExcludes() {
            return exportExcludes;
        }

        public void setExportExcludes(String exportExcludes) {
            this.exportExcludes = exportExcludes;
        }

        @Input
        public boolean getAddServiceUses() {
            return addServiceUses;
        }

        public void setAddServiceUses(boolean addServiceUses) {
            this.addServiceUses = addServiceUses;
        }

        @Override
        public String toString() {
            return "GenerateModuleInfo{" +
                    "workingDirectory=" + workingDirectory +
                    ", outputDirectory=" + outputDirectory +
                    ", modules=" + modules +
                    ", jdepsExtraArgs=" + jdepsExtraArgs +
                    ", artifact='" + artifact + '\'' +
                    ", additionalDependencies='" + additionalDependencies + '\'' +
                    ", moduleName='" + moduleName + '\'' +
                    ", exportExcludes='" + exportExcludes + '\'' +
                    ", addServiceUses=" + addServiceUses +
                    ", project=" + project +
                    '}';
        }
    }

    public static class AddModuleInfo{
        private String artifactId;

        private String version;

        private File workingDirectory;

        private File buildDirectory;

        private File outputDirectory;

        private boolean overwriteExistingFiles;

        private MainModule mainModule;

        private List<AddModule> modules = new LinkedList<>();

        private List<String> jdepsExtraArgs = new LinkedList<>();

        @Internal
        private Project project;

        public AddModuleInfo(Project project){
            this.project = project;
            this.mainModule = new MainModule(project);
        }

        @Internal
        public String getArtifactId() {
            return artifactId;
        }

        public void setArtifactId(String artifactId) {
            this.artifactId = artifactId;
        }

        @Internal
        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        @Internal
        public File getWorkingDirectory() {
            return workingDirectory;
        }

        public void setWorkingDirectory(File workingDirectory) {
            this.workingDirectory = workingDirectory;
        }

        @Internal
        public File getBuildDirectory() {
            return buildDirectory;
        }

        public void setBuildDirectory(File buildDirectory) {
            this.buildDirectory = buildDirectory;
        }

        @Internal
        public List<AddModule> getModules() {
            return modules;
        }

        @Input
        public File getOutputDirectory() {
            return outputDirectory;
        }

        public void setOutputDirectory(File outputDirectory) {
            this.outputDirectory = outputDirectory;
        }

        @Input
        public boolean isOverwriteExistingFiles() {
            return overwriteExistingFiles;
        }

        public void setOverwriteExistingFiles(boolean overwriteExistingFiles) {
            this.overwriteExistingFiles = overwriteExistingFiles;
        }

        @Input
        public MainModule getMainModule() {
            return mainModule;
        }

        public void mainModule(Closure closure){
            project.configure(mainModule,closure);
        }

        public void module(Closure closure){
            final AddModule module = new AddModule(project);
            project.configure(module,closure);
            modules.add(module);
        }

        @Input
        public List<String> getJdepsExtraArgs() {
            return jdepsExtraArgs;
        }

        @Override
        public String toString() {
            return "AddModuleInfo{" +
                    "artifactId='" + artifactId + '\'' +
                    ", version='" + version + '\'' +
                    ", workingDirectory=" + workingDirectory +
                    ", buildDirectory=" + buildDirectory +
                    ", outputDirectory=" + outputDirectory +
                    ", overwriteExistingFiles=" + overwriteExistingFiles +
                    ", mainModule=" + mainModule +
                    ", modules=" + modules +
                    ", jdepsExtraArgs=" + jdepsExtraArgs +
                    ", project=" + project +
                    '}';
        }
    }

    public static class MainModule implements CoreConverter<MainModuleConfiguration>{
        private ModuleInfo moduleInfo = new ModuleInfo();
        private File moduleInfoFile;
        private String moduleInfoSource;
        private String mainClass;

        @Internal
        private Project project;

        public MainModule(Project project){
            this.project = project;
        }
        @Input
        public ModuleInfo getModuleInfo() {
            return moduleInfo;
        }

        public void moduleInfo(Closure closure){
            project.configure(moduleInfo,closure);
        }
        @Input
        public File getModuleInfoFile() {
            return moduleInfoFile;
        }

        public void setModuleInfoFile(File moduleInfoFile) {
            this.moduleInfoFile = moduleInfoFile;
        }
        @Input
        public String getModuleInfoSource() {
            return moduleInfoSource;
        }

        public void setModuleInfoSource(String moduleInfoSource) {
            this.moduleInfoSource = moduleInfoSource;
        }
        @Input
        public String getMainClass() {
            return mainClass;
        }

        public void setMainClass(String mainClass) {
            this.mainClass = mainClass;
        }

        @Override
        public String toString() {
            return "MainModule{" +
                    "moduleInfo=" + moduleInfo +
                    ", moduleInfoFile=" + moduleInfoFile +
                    ", moduleInfoSource='" + moduleInfoSource + '\'' +
                    ", mainClass='" + mainClass + '\'' +
                    '}';
        }

        @Override
        public MainModuleConfiguration toCoreObject() {
            final MainModuleConfiguration mainModuleConfiguration = new MainModuleConfiguration();
            mainModuleConfiguration.setModuleInfo(moduleInfo.toCoreObject());
            mainModuleConfiguration.setMainClass(mainClass);
            mainModuleConfiguration.setModuleInfoFile(moduleInfoFile);
            return mainModuleConfiguration;
        }
    }

    static class ModuleInfo implements CoreConverter<ModuleInfoConfiguration>{
        private String requires = "*;";
        private String exports = "*;";
        private String opens = "!*;";
        private String uses;
        private String name;
        private boolean addServiceUses;
        private boolean open;

        public ModuleInfo(){
        }

        @Input
        public String getRequires() {
            return requires;
        }

        public void setRequires(String requires) {
            this.requires = requires;
        }
        @Input
        public String getExports() {
            return exports;
        }

        public void setExports(String exports) {
            this.exports = exports;
        }
        @Input
        public String getOpens() {
            return opens;
        }

        public void setOpens(String opens) {
            this.opens = opens;
        }
        @Input
        public String getUses() {
            return uses;
        }

        public void setUses(String uses) {
            this.uses = uses;
        }
        @Input
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
        @Input
        public boolean isAddServiceUses() {
            return addServiceUses;
        }

        public void setAddServiceUses(boolean addServiceUses) {
            this.addServiceUses = addServiceUses;
        }
        @Input
        public boolean isOpen() {
            return open;
        }

        public void setOpen(boolean open) {
            this.open = open;
        }

        @Override
        public String toString() {
            return "ModuleInfo{" +
                    "requires='" + requires + '\'' +
                    ", exports='" + exports + '\'' +
                    ", opens='" + opens + '\'' +
                    ", uses='" + uses + '\'' +
                    ", name='" + name + '\'' +
                    ", addServiceUses=" + addServiceUses +
                    ", open=" + open +
                    '}';
        }

        @Override
        public ModuleInfoConfiguration toCoreObject() {
            final ModuleInfoConfiguration moduleInfoConfiguration = new ModuleInfoConfiguration();
            moduleInfoConfiguration.setAddServiceUses(addServiceUses);
            moduleInfoConfiguration.setExports(exports);
            moduleInfoConfiguration.setName(name);
            moduleInfoConfiguration.setOpen(open);
            moduleInfoConfiguration.setOpens(opens);
            moduleInfoConfiguration.setRequires(requires);
            moduleInfoConfiguration.setUses(uses);
            return moduleInfoConfiguration;
        }
    }

    public static class Module implements CoreConverter<ModuleConfiguration>{
        private ModuleArtifact artifact = new ModuleArtifact();
        private List<ModuleArtifact> additionalDependencies = new LinkedList<>();
        private List<ModuleArtifact> excludeDependencies = new LinkedList<>();
        private ModuleInfo moduleInfo = new ModuleInfo();
        private boolean includeOptional;

        @Internal
        private Project project;

        public Module(Project project){
            this.project = project;
            this.includeOptional = true;
        }

        @Input
        public ModuleArtifact getArtifact() {
            return artifact;
        }

        public void artifact(Closure closure){
            project.configure(artifact,closure);
        }

        @Internal
        public List<ModuleArtifact> getAdditionalDependencies() {
            return additionalDependencies;
        }

        public void additionalDependency(Closure closure){
            final ModuleArtifact moduleArtifact = new ModuleArtifact();
            project.configure(moduleArtifact,closure);
            additionalDependencies.add(moduleArtifact);
        }

        @Internal
        public List<ModuleArtifact> getExcludeDependencies() {
            return excludeDependencies;
        }

        public void excludeDependency(Closure closure){
            final ModuleArtifact moduleArtifact = new ModuleArtifact();
            project.configure(moduleArtifact,closure);
            excludeDependencies.add(moduleArtifact);
        }

        @Input
        public ModuleInfo getModuleInfo() {
            return moduleInfo;
        }

        public void moduleInfo(Closure closure){
            project.configure(moduleInfo,closure);
        }


        @Input
        public boolean getIncludeOptional() {
            return includeOptional;
        }

        public void setIncludeOptional(boolean includeOptional) {
            this.includeOptional = includeOptional;
        }


        @Override
        public String toString() {
            return "Module{" +
                    "artifact=" + artifact +
                    ", additionalDependencies=" + additionalDependencies +
                    ", excludeDependencies=" + excludeDependencies +
                    ", moduleInfo=" + moduleInfo +
                    ", project=" + project +
                    ", includeOptional=" + includeOptional +
                    '}';
        }

        @Override
        public ModuleConfiguration toCoreObject() {
            final ModuleConfiguration moduleConfiguration = new ModuleConfiguration();
            moduleConfiguration.setModuleInfo(getModuleInfo().toCoreObject());
            moduleConfiguration.setArtifact(getArtifact().toCoreObject());
            moduleConfiguration.getAdditionalDependencies().clear();
            moduleConfiguration.getAdditionalDependencies().addAll(
                    getAdditionalDependencies().stream().map(aad -> aad.toCoreObject()).collect(Collectors.toList()));
            moduleConfiguration.getExcludeDependencies().clear();
            moduleConfiguration.getExcludeDependencies().addAll(
                    getExcludeDependencies().stream().map(ead -> ead.toCoreObject()).collect(Collectors.toList()));
            moduleConfiguration.setIncludeOptional(includeOptional);
            return moduleConfiguration;
        }
    }

    public static class AddModule extends Module{
        private File file;
        private File moduleInfoFile;
        private String moduleInfoSource;
        private String mainClass;
        private String version;

        @Internal
        private Project project;

        public AddModule(Project project){
            super(project);
            this.project = project;
        }

        @Input
        public File getFile() {
            return file;
        }

        public void setFile(File file) {
            this.file = file;
        }
        @Input
        public File getModuleInfoFile() {
            return moduleInfoFile;
        }

        public void setModuleInfoFile(File moduleInfoFile) {
            this.moduleInfoFile = moduleInfoFile;
        }
        @Input
        public String getModuleInfoSource() {
            return moduleInfoSource;
        }

        public void setModuleInfoSource(String moduleInfoSource) {
            this.moduleInfoSource = moduleInfoSource;
        }
        @Input
        public String getMainClass() {
            return mainClass;
        }

        public void setMainClass(String mainClass) {
            this.mainClass = mainClass;
        }
        @Input
        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        @Override
        public String toString() {
            return "AddModule{" +
                    "artifact=" + getArtifact() +
                    ", additionalDependencies=" + getAdditionalDependencies() +
                    ", excludeDependencies=" + getExcludeDependencies() +
                    ", moduleInfo=" + getModuleInfo() +
                    ", includeOptional=" + getIncludeOptional() +
                    ", file=" + file +
                    ", moduleInfoFile=" + moduleInfoFile +
                    ", moduleInfoSource='" + moduleInfoSource + '\'' +
                    ", mainClass='" + mainClass + '\'' +
                    ", version='" + version + '\'' +
                    '}';
        }

        @Override
        public AddModuleConfiguration toCoreObject() {
            final AddModuleConfiguration addModuleConfiguration = new AddModuleConfiguration();
            addModuleConfiguration.setFile(file);
            addModuleConfiguration.setMainClass(mainClass);
            addModuleConfiguration.setModuleInfoFile(moduleInfoFile);
            addModuleConfiguration.setModuleInfoSource(moduleInfoSource);
            addModuleConfiguration.setVersion(version);
            addModuleConfiguration.setModuleInfo(getModuleInfo().toCoreObject());
            addModuleConfiguration.setArtifact(getArtifact().toCoreObject());
            addModuleConfiguration.getAdditionalDependencies().clear();
            addModuleConfiguration.getAdditionalDependencies().addAll(
                    getAdditionalDependencies().stream().map(ad -> ad.toCoreObject()).collect(Collectors.toList()));
            addModuleConfiguration.getExcludeDependencies().clear();
            addModuleConfiguration.getExcludeDependencies().addAll(
                    getExcludeDependencies().stream().map(ead -> ead.toCoreObject()).collect(Collectors.toList()));
            addModuleConfiguration.setIncludeOptional(getIncludeOptional());
            return addModuleConfiguration;
        }
    }

    static class ModuleArtifact implements CoreConverter<ArtifactConfiguration>{
        private String groupId;
        private String artifactId;
        private String version;
        private String classifier;
        private String type;

        @Input
        public String getGroupId() {
            return groupId;
        }

        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }
        @Input
        public String getArtifactId() {
            return artifactId;
        }

        public void setArtifactId(String artifactId) {
            this.artifactId = artifactId;
        }
        @Input
        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }
        @Input
        public String getClassifier() {
            return classifier;
        }

        public void setClassifier(String classifier) {
            this.classifier = classifier;
        }
        @Input
        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return "ModuleArtifact{" +
                    "groupId='" + groupId + '\'' +
                    ", artifactId='" + artifactId + '\'' +
                    ", version='" + version + '\'' +
                    ", classifier='" + classifier + '\'' +
                    ", type='" + type + '\'' +
                    '}';
        }

        public ArtifactConfiguration toCoreObject(){
            final ArtifactConfiguration artifactConfiguration = new ArtifactConfiguration();
            artifactConfiguration.setArtifactId(artifactId);
            artifactConfiguration.setGroupId(groupId);
            artifactConfiguration.setClassifier(classifier);
            artifactConfiguration.setType(type);
            artifactConfiguration.setVersion(version);
            artifactConfiguration.setDependencyString(artifactConfiguration.toDependencyString());
            return artifactConfiguration;
        }
    }

    interface CoreConverter<T>{
        T toCoreObject();
    }
}
