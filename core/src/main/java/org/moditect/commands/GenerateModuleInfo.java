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
package org.moditect.commands;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.moditect.model.DependencePattern;
import org.moditect.model.DependencyDescriptor;
import org.moditect.model.GeneratedModuleInfo;
import org.moditect.model.PackageNamePattern;
import org.moditect.spi.log.Log;

public class GenerateModuleInfo {

    public GenerateModuleInfo(
                              Path inputJar, String moduleName, boolean open,
                              Set<DependencyDescriptor> dependencies, List<PackageNamePattern> exportPatterns,
                              List<PackageNamePattern> opensPatterns, List<DependencePattern> requiresPatterns,
                              Path workingDirectory, Path outputDirectory,
                              Set<String> opensResources, Set<String> uses, Set<String> provides,
                              boolean addServiceUses, List<String> jdepsExtraArgs, Log log) {
        throw new UnsupportedOperationException("new GenerateModuleInfo() requires Java 9");
    }

    public static Path createCopyWithAutoModuleNameManifestHeader(Path workingDirectory, Path inputJar, String moduleName) {
        throw new UnsupportedOperationException("GenerateModuleInfo.createCopyWithAutoModuleNameManifestHeader() requires Java 9");
    }

    public GeneratedModuleInfo run() {
        throw new UnsupportedOperationException("new GenerateModuleInfo().run() requires Java 9");
    }

}
