/*
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Copyright The original authors
 *
 *  Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.moditect.model;

import java.lang.module.FindException;
import java.lang.module.ModuleFinder;
import java.nio.file.Path;

public class DependencyDescriptor {

    private final Path path;
    private final boolean optional;

    /**
     * The original (automatic) module name of that dependency.
     */
    private final String originalModuleName;

    /**
     * The module name of that dependency as assigned during the current modularization build.
     */
    private final String assignedModuleName;

    public DependencyDescriptor(Path path, boolean optional, String assignedModuleName) {
        this.path = path;
        this.optional = optional;
        this.originalModuleName = getAutoModuleNameFromInputJar(path, assignedModuleName);
        if (this.originalModuleName == null) {
            throw new IllegalArgumentException("No assignedModuleName provided for jar with invalid module name: " + path);
        }
        this.assignedModuleName = assignedModuleName;
    }

    public static String getAutoModuleNameFromInputJar(Path path, String invalidModuleName) {
        try {
            return ModuleFinder.of(path)
                    .findAll()
                    .iterator()
                    .next()
                    .descriptor()
                    .name();
        }
        catch (FindException e) {
            if (e.getCause() != null && e.getCause().getMessage().contains("Invalid module name")) {
                return invalidModuleName;
            }
            throw e;
        }
    }

    public Path getPath() {
        return path;
    }

    public boolean isOptional() {
        return optional;
    }

    public String getOriginalModuleName() {
        return originalModuleName;
    }

    public String getAssignedModuleName() {
        return assignedModuleName;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + path.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DependencyDescriptor other = (DependencyDescriptor) obj;
        return path.equals(other.path);
    }

    @Override
    public String toString() {
        return "DependencyDescriptor [path=" + path + ", optional=" + optional + ", originalModuleName=" + originalModuleName + ", assignedModuleName="
                + assignedModuleName + "]";
    }
}
