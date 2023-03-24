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
package org.moditect.mavenplugin.add.model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.aether.artifact.Artifact;
import org.moditect.mavenplugin.common.model.ArtifactConfiguration;
import org.moditect.mavenplugin.common.model.ModuleInfoConfiguration;

/**
 * @author Gunnar Morling
 *
 */
public class ModuleConfiguration {

    private File file;
    private ArtifactConfiguration artifact;
    private List<ArtifactConfiguration> additionalDependencies = new ArrayList<>();

    private ModuleInfoConfiguration moduleInfo;
    private File moduleInfoFile;
    private String moduleInfoSource;

    private String mainClass;
    private String version;

    private Artifact resolvedArtifact;

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public ArtifactConfiguration getArtifact() {
        return artifact;
    }

    public void setArtifact(ArtifactConfiguration artifact) {
        this.artifact = artifact;
    }

    public List<ArtifactConfiguration> getAdditionalDependencies() {
        return additionalDependencies;
    }

    public void setAdditionalDependencies(List<ArtifactConfiguration> additionalDependencies) {
        this.additionalDependencies = additionalDependencies;
    }

    public ModuleInfoConfiguration getModuleInfo() {
        return moduleInfo;
    }

    public void setModuleInfo(ModuleInfoConfiguration moduleInfo) {
        this.moduleInfo = moduleInfo;
    }

    public File getModuleInfoFile() {
        return moduleInfoFile;
    }

    public void setModuleInfoFile(File moduleInfoFile) {
        this.moduleInfoFile = moduleInfoFile;
    }

    public String getModuleInfoSource() {
        return moduleInfoSource;
    }

    public void setModuleInfoSource(String moduleInfoSource) {
        this.moduleInfoSource = moduleInfoSource;
    }

    public String getMainClass() {
        return mainClass;
    }

    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Artifact getResolvedArtifact() {
        if (resolvedArtifact == null) {
            throw new IllegalStateException("Artifact must be resolved first");
        }

        return resolvedArtifact;
    }

    public void setResolvedArtifact(Artifact resolvedArtifact) {
        this.resolvedArtifact = resolvedArtifact;
    }

    @Override
    public String toString() {
        return "ModuleConfiguration [artifact=" + artifact + ", moduleInfo=" + moduleInfo + ", moduleInfoFile="
                + moduleInfoFile + ", moduleInfoSource=" + moduleInfoSource + ", mainClass=" + mainClass
                + ", version=" + version + "]";
    }
}
