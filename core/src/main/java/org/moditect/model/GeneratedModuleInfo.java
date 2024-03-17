/*
 *  SPDX-License-Identifier: Apache-2.0
 *
 *   Copyright The original authors
 *
 *   Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.moditect.model;

import java.nio.file.Path;

public class GeneratedModuleInfo {

    private final String moduleName;
    private final Path path;

    public GeneratedModuleInfo(String moduleName, Path path) {
        this.moduleName = moduleName;
        this.path = path;
    }

    public String getModuleName() {
        return moduleName;
    }

    public Path getPath() {
        return path;
    }

    @Override
    public String toString() {
        return "GeneratedModuleInfo[moduleName=" + moduleName + ", path=" + path + "]";
    }
}
