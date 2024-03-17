/*
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Copyright The original authors
 *
 *  Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
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
