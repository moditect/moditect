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
package org.moditect.mavenplugin.common.model;

/**
 * @author Gunnar Morling
 */
public class ArtifactConfiguration {

    private String groupId;
    private String artifactId;
    private String version;
    private String classifier;
    private String type;
    private String dependencyString;

    public ArtifactConfiguration() {
    }

    public ArtifactConfiguration(String dependencyString) {
        this.dependencyString = dependencyString;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getClassifier() {
        return classifier;
    }

    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    public String getType() {
        return type != null ? type : "jar";
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "ArtifactConfiguration [groupId=" + groupId + ", artifactId=" + artifactId + ", version=" + version + ", classifier=" + classifier + ", type=" + type + "]";
    }

    public void setDependencyString(String dependencyString) {
        this.dependencyString = dependencyString;
    }

    public String toDependencyString() {
        if ( this.dependencyString != null ) {
            return dependencyString;
        }

        String dependencyString = groupId + ":" + artifactId + ":" + version;

        if ( classifier != null ) {
            dependencyString += ":" + classifier;
        }

        if ( type != null ) {
            dependencyString += ":" + type;
        }

        return dependencyString;
    }
}
