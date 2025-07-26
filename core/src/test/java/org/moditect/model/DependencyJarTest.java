/*
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Copyright The original authors
 *
 *  Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.moditect.model;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.moditect.model.DependencyJar.FileCopy;

import junit.framework.TestCase;

/**
 * Exercises {@link DependencyJar}
 */
public class DependencyJarTest extends TestCase {

    /**
     * Demonstrates the behaviour of {@link DependencyJar#uniqueDestinations(Path, Set)}:
     * <ul>
     *   <li>unique dependency jar names are copied directly</li>
     *   <li>non-unique jar names are disambiguated by prepending the groupId</li>
     * </ul>
     */
    @Test
    public void testUniqueDestinations() {
        Set<DependencyJar> deps = new HashSet<>(Arrays.asList(
                new DependencyJar("ga", "aa", "va", Paths.get("ga/aa-va.jar")),
                new DependencyJar("gr1", "ar", "vr", Paths.get("gr1/ar-vr.jar")),
                new DependencyJar("gb", "ab", "vb", Paths.get("gb/ab-vb.jar")),
                new DependencyJar("gr2", "ar", "vr", Paths.get("gr2/ar-vr.jar")),
                new DependencyJar("gc", "ac", "vc", Paths.get("gc/ac-vc.jar"))));

        List<FileCopy> copies = DependencyJar.uniqueDestinations(Paths.get("dst"), deps);

        Assert.assertEquals(""
                + "ga/aa-va.jar -> dst/aa-va.jar\n"
                + "gb/ab-vb.jar -> dst/ab-vb.jar\n"
                + "gc/ac-vc.jar -> dst/ac-vc.jar\n"
                + "gr1/ar-vr.jar -> dst/gr1.ar-vr.jar\n"
                + "gr2/ar-vr.jar -> dst/gr2.ar-vr.jar",
                copies.stream()
                        .map(fc -> fc.source() + " -> " + fc.sink())
                        .sorted()
                        .collect(Collectors.joining("\n")));
    }
}
