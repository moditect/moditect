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
package org.moditect.mavenplugin.add.model;

import java.io.File;

import org.moditect.mavenplugin.common.model.ArtifactConfiguration;
import org.moditect.mavenplugin.common.model.ModuleInfoConfiguration;

/**
 * @author Gunnar Morling
 *
 */
public class ModuleConfiguration {

    private File file;
    private ArtifactConfiguration artifact;

    private ModuleInfoConfiguration moduleInfo;
    private File moduleInfoFile;
    private String moduleInfoSource;

    private String mainClass;
    private String version;

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

    @Override
    public String toString() {
        return "ModuleConfiguration [artifact=" + artifact + ", moduleInfo=" + moduleInfo + ", moduleInfoFile="
                + moduleInfoFile + ", moduleInfoSource=" + moduleInfoSource + ", mainClass=" + mainClass
                + ", version=" + version + "]";
    }
}
