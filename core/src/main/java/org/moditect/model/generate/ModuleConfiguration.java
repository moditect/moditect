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
package org.moditect.model.generate;

import java.util.ArrayList;
import java.util.List;

import org.moditect.model.common.ArtifactConfiguration;
import org.moditect.model.common.ModuleInfoConfiguration;

/**
 * @author Gunnar Morling
 * @author Pratik Parikh
 */
public class ModuleConfiguration {

    private ArtifactConfiguration artifact;
    private List<ArtifactConfiguration> additionalDependencies = new ArrayList<>();
    private List<ArtifactConfiguration> excludeDependencies = new ArrayList<>();
    private ModuleInfoConfiguration moduleInfo = new ModuleInfoConfiguration();
    private boolean includeOptional;

    public ModuleConfiguration(){
        includeOptional = true;
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

    public List<ArtifactConfiguration> getExcludeDependencies() {
        return excludeDependencies;
    }

    public void setExcludeDependencies(List<ArtifactConfiguration> excludeDependencies) {
        this.excludeDependencies = excludeDependencies;
    }

    public ModuleInfoConfiguration getModuleInfo() {
        return moduleInfo;
    }

    public void setModuleInfo(ModuleInfoConfiguration moduleInfo) {
        this.moduleInfo = moduleInfo;
    }

    public boolean getIncludeOptional() {
        return includeOptional;
    }

    public void setIncludeOptional(boolean includeOptional) {
        this.includeOptional = includeOptional;
    }

    @Override
    public String toString() {
        return "ModuleConfiguration{" +
                "artifact=" + artifact +
                ", additionalDependencies=" + additionalDependencies +
                ", excludeDependencies=" + excludeDependencies +
                ", moduleInfo=" + moduleInfo +
                ", includeOptional=" + includeOptional +
                '}';
    }
}
