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
package org.moditect.model;

import java.lang.module.FindException;
import java.lang.module.ModuleFinder;
import java.nio.file.Path;

public class DependencyDescriptor {

    private Path path;
    private final boolean optional;

    /**
     * The original (automatic) module name of that dependency.
     */
    private String originalModuleName;

    /**
     * The module name of that dependency as assigned during the current modularization build.
     */
    private final String assignedModuleName;

    public DependencyDescriptor(Path path, boolean optional, String assignedModuleName) {
        this.path = path;
        this.optional = optional;
        try {
            this.originalModuleName =  ModuleFinder.of(path)
                    .findAll()
                    .iterator()
                    .next()
                    .descriptor()
                    .name();
        }
        catch (FindException e) {
            if ( e.getCause() != null && e.getCause().getMessage().contains( "Invalid module name" ) ) {
                this.originalModuleName = assignedModuleName;
            }
        }
        this.assignedModuleName = assignedModuleName;
    }

    public Path getPath() {
        return path;
    }

    public void setPath(final Path path){
        this.path = path;
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
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( getClass() != obj.getClass() )
            return false;
        DependencyDescriptor other = (DependencyDescriptor) obj;
        return path.equals( other.path );
    }

    @Override
    public String toString() {
        return "DependencyDescriptor [path=" + path + ", optional=" + optional + ", originalModuleName=" + originalModuleName + ", assignedModuleName=" + assignedModuleName + "]";
    }
}
