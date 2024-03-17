/*
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Copyright The original authors
 *
 *  Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.moditect.model;

public enum JarInclusionPolicy {
    NONE,
    APP,
    APP_WITH_DEPENDENCIES;

    public boolean includeAppJar() {
        return this == APP || this == APP_WITH_DEPENDENCIES;
    }

    public boolean includeDependencies() {
        return this == APP_WITH_DEPENDENCIES;
    }
}
