/*
 *  SPDX-License-Identifier: Apache-2.0
 *
 *   Copyright The original authors
 *
 *   Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
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
