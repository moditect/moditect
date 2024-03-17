/*
 *  SPDX-License-Identifier: Apache-2.0
 *
 *   Copyright The original authors
 *
 *   Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.moditect.test.model;

import java.util.List;
import java.util.regex.Pattern;

import org.junit.Test;
import org.moditect.model.PackageNamePattern;

import static org.assertj.core.api.Assertions.assertThat;

public class PackageNamePatternTest {

    @Test
    public void includeAll() {
        PackageNamePattern pattern = PackageNamePattern.parsePattern("*");
        assertThat(pattern.getKind()).isEqualTo(PackageNamePattern.Kind.INCLUSIVE);
        assertThat(pattern.getPattern().pattern()).isEqualTo(Pattern.compile(".*").pattern());
        assertThat(pattern.getTargetModules()).isEmpty();
    }

    @Test
    public void excludeAll() {
        PackageNamePattern pattern = PackageNamePattern.parsePattern("!*");
        assertThat(pattern.getKind()).isEqualTo(PackageNamePattern.Kind.EXCLUSIVE);
        assertThat(pattern.getPattern().pattern()).isEqualTo(Pattern.compile(".*").pattern());
        assertThat(pattern.getTargetModules()).isEmpty();
    }

    @Test
    public void qualifiedInclude() {
        PackageNamePattern pattern = PackageNamePattern.parsePattern("org.hibernate.validator.internal.util.logging to org.jboss.logging");
        assertThat(pattern.getKind()).isEqualTo(PackageNamePattern.Kind.INCLUSIVE);
        assertThat(pattern.getPattern().pattern()).isEqualTo(Pattern.compile("org\\.hibernate\\.validator\\.internal\\.util\\.logging").pattern());
        assertThat(pattern.getTargetModules()).containsExactly("org.jboss.logging");
    }

    @Test
    public void excludeWithWildcard() {
        PackageNamePattern pattern = PackageNamePattern.parsePattern("!org.hibernate.validator.internal*");
        assertThat(pattern.getKind()).isEqualTo(PackageNamePattern.Kind.EXCLUSIVE);
        assertThat(pattern.getPattern().pattern()).isEqualTo(Pattern.compile("org\\.hibernate\\.validator\\.internal.*").pattern());
        assertThat(pattern.getTargetModules()).isEmpty();
    }

    @Test
    public void parsePatterns() {
        StringBuilder patterns = new StringBuilder();
        patterns.append("org.hibernate.validator.internal.util.logging to org.jboss.logging;\n");
        patterns.append("!org.hibernate.validator.internal*;\n");
        patterns.append("*;\n");

        List<PackageNamePattern> patternList = PackageNamePattern.parsePatterns(patterns.toString());
        assertThat(patternList).hasSize(3);

        PackageNamePattern pattern = patternList.get(0);
        assertThat(pattern.getKind()).isEqualTo(PackageNamePattern.Kind.INCLUSIVE);
        assertThat(pattern.getPattern().pattern()).isEqualTo(Pattern.compile("org\\.hibernate\\.validator\\.internal\\.util\\.logging").pattern());
        assertThat(pattern.getTargetModules()).containsExactly("org.jboss.logging");

        pattern = patternList.get(1);
        assertThat(pattern.getKind()).isEqualTo(PackageNamePattern.Kind.EXCLUSIVE);
        assertThat(pattern.getPattern().pattern()).isEqualTo(Pattern.compile("org\\.hibernate\\.validator\\.internal.*").pattern());
        assertThat(pattern.getTargetModules()).isEmpty();

        pattern = patternList.get(2);
        assertThat(pattern.getKind()).isEqualTo(PackageNamePattern.Kind.INCLUSIVE);
        assertThat(pattern.getPattern().pattern()).isEqualTo(Pattern.compile(".*").pattern());
        assertThat(pattern.getTargetModules()).isEmpty();
    }
}
