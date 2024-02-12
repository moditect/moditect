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
package org.moditect.mavenplugin.generate;

import java.util.List;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.DependencyCollectionContext;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.util.graph.selector.ScopeDependencySelector;

/**
 * A dependency selector which emulates the dependency selection at the point of
 * compilation of an existing artifact: direct provided-scoped dependencies are
 * included, but not transitive provided-scoped ones. Test-scoped dependencies are
 * not included.
 *
 * @author Gunnar Morling
 */
public class CompileScopeDependencySelector implements DependencySelector {

    private boolean level1 = true;
    private DependencySelector delegate = new ScopeDependencySelector("test").deriveChildSelector(new MockDependencyCollectionContext());

    @Override
    public boolean selectDependency(Dependency dependency) {
        return delegate.selectDependency(dependency);
    }

    @Override
    public DependencySelector deriveChildSelector(DependencyCollectionContext context) {
        if (level1) {
            level1 = false;
            return this;
        }
        else {
            return new ScopeDependencySelector("test", "provided").deriveChildSelector(new MockDependencyCollectionContext());
        }
    }

    // TODO get rid of this; it's needed only to get an instance of
    // ScopeDependencySelector in "transitive" mode
    private static class MockDependencyCollectionContext implements DependencyCollectionContext {

        @Override
        public RepositorySystemSession getSession() {
            return null;
        }

        @Override
        public Artifact getArtifact() {
            return null;
        }

        @Override
        public Dependency getDependency() {
            return new Dependency(new DefaultArtifact("com.example:example:1.0"), null);
        }

        @Override
        public List<Dependency> getManagedDependencies() {
            return null;
        }
    }
}
