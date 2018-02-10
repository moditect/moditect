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
package org.moditect.dependency;

import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.moditect.commands.AddModuleInfo;
import org.moditect.generator.ModuleInfoGenerator;
import org.moditect.model.DependencyDescriptor;
import org.moditect.model.GeneratedModuleInfo;
import org.moditect.model.add.MainModuleConfiguration;
import org.moditect.model.common.ArtifactConfiguration;
import org.moditect.model.generate.ArtifactIdentifier;
import org.moditect.spi.log.Log;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Build tools specific resolvers.
 *
 * @author Pratik Parikh
 */
public interface ArtifactResolver {

    String getVersionFromProject(Artifact artifact) throws Exception;

    DefaultRepositorySystemSession newSession();

    Set<DependencyDescriptor> getDependencyDescriptors(final Map<ArtifactIdentifier, String> assignedNamesByModule);

    String determineVersion(ArtifactConfiguration artifact) throws Exception;

    Path getSourceDirectory(Path defaultDirectory);

    void postActivity(AddModuleInfo addModuleInfo, MainModuleConfiguration module,
                      ModuleInfoGenerator moduleInfoGenerator, Map<ArtifactIdentifier, String> assignedNamesByModule,
                      Map<ArtifactIdentifier, Path> modularizedJars) throws Exception;
}
