/*
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Copyright The original authors
 *
 *  Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.moditect.mavenplugin.add.model;

import java.io.File;

import org.moditect.mavenplugin.common.model.ModuleInfoConfiguration;

/**
 * @author Gunnar Morling
 *
 */
public class MainModuleConfiguration {

    private ModuleInfoConfiguration moduleInfo;
    private File moduleInfoFile;
    private String moduleInfoSource;
    private String mainClass;

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

    @Override
    public String toString() {
        return "MainModuleConfiguration [ moduleInfo=" + moduleInfo + ", moduleInfoFile="
                + moduleInfoFile + ", moduleInfoSource=" + moduleInfoSource + ", mainClass=" + mainClass
                + "]";
    }
}
