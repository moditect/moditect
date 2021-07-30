/*
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
