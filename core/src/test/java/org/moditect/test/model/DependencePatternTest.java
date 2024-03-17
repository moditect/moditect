/*
 *  SPDX-License-Identifier: Apache-2.0
 *
 *   Copyright The original authors
 *
 *   Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.moditect.test.model;

import java.util.regex.Pattern;

import org.junit.Test;
import org.moditect.model.DependencePattern;

import static org.assertj.core.api.Assertions.assertThat;

public class DependencePatternTest {

    @Test
    public void all() {
        DependencePattern pattern = DependencePattern.parsePattern("*");
        assertThat(pattern.getPattern().pattern()).isEqualTo(Pattern.compile(".*").pattern());
        assertThat(pattern.getModifiers()).isEmpty();
    }

    @Test
    public void noModifiers() {
        DependencePattern pattern = DependencePattern.parsePattern("java.validation");
        assertThat(pattern.getPattern().pattern()).isEqualTo(Pattern.compile("java\\.validation").pattern());
        assertThat(pattern.getModifiers()).isEmpty();
    }

    @Test
    public void oneModifier() {
        DependencePattern pattern = DependencePattern.parsePattern("static java.validation");
        assertThat(pattern.getPattern().pattern()).isEqualTo(Pattern.compile("java\\.validation").pattern());
        assertThat(pattern.getModifiers()).containsOnly("static");
    }

    @Test
    public void twoModifiers() {
        DependencePattern pattern = DependencePattern.parsePattern("static transitive java.validation");
        assertThat(pattern.getPattern().pattern()).isEqualTo(Pattern.compile("java\\.validation").pattern());
        assertThat(pattern.getModifiers()).containsOnly("static", "transitive");
    }
}
