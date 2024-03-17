/*
 *  SPDX-License-Identifier: Apache-2.0
 *
 *   Copyright The original authors
 *
 *   Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
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
        return "ArtifactConfiguration [groupId=" + groupId + ", artifactId=" + artifactId + ", version=" + version + ", classifier=" + classifier + ", type=" + type
                + "]";
    }

    public void setDependencyString(String dependencyString) {
        this.dependencyString = dependencyString;
    }

    public String toDependencyString() {
        if (this.dependencyString != null) {
            return dependencyString;
        }

        String dependencyString = groupId + ":" + artifactId + ":" + version;

        if (classifier != null) {
            dependencyString += ":" + classifier;
        }

        if (type != null) {
            dependencyString += ":" + type;
        }

        return dependencyString;
    }
}
