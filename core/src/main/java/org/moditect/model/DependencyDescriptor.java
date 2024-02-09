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
package org.moditect.model;

import java.nio.file.Path;

public class DependencyDescriptor {

    public DependencyDescriptor(Path path, boolean optional, String assignedModuleName) {
        throw new UnsupportedOperationException("new DependencyDescriptor() requires Java 9");
    }

    public static String getAutoModuleNameFromInputJar(Path path, String invalidModuleName) {
        throw new UnsupportedOperationException("getAutoModuleNameFromInputJar() requires Java 9");
    }
}
