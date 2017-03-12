/**
 *  Copyright 2017 Gunnar Morling (http://www.gunnarmorling.de/)
 *  and/or other contributors as indicated by the @authors tag. See the
 *  copyright.txt file in the distribution for a full listing of all
 *  contributors.
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

    private final Path path;
    private final boolean optional;

    public DependencyDescriptor(Path path, boolean optional) {
        this.path = path;
        this.optional = optional;
    }

    public Path getPath() {
        return path;
    }

    public boolean isOptional() {
        return optional;
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
        return "DependencyDescriptor [path=" + path + ", optional=" + optional + "]";
    }
}
