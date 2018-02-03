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
package org.moditect.test.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.regex.Pattern;

import org.junit.Test;
import org.moditect.model.PackageNamePattern;

public class PackageNamePatternTest {

    @Test
    public void includeAll() {
        PackageNamePattern pattern = PackageNamePattern.parsePattern( "*" );
        assertThat( pattern.getKind() ).isEqualTo( PackageNamePattern.Kind.INCLUSIVE );
        assertThat( pattern.getPattern().pattern() ).isEqualTo( Pattern.compile( ".*" ).pattern() );
        assertThat( pattern.getTargetModules() ).isEmpty();
    }

    @Test
    public void excludeAll() {
        PackageNamePattern pattern = PackageNamePattern.parsePattern( "!*" );
        assertThat( pattern.getKind() ).isEqualTo( PackageNamePattern.Kind.EXCLUSIVE );
        assertThat( pattern.getPattern().pattern() ).isEqualTo( Pattern.compile( ".*" ).pattern() );
        assertThat( pattern.getTargetModules() ).isEmpty();
    }

    @Test
    public void qualifiedInclude() {
        PackageNamePattern pattern = PackageNamePattern.parsePattern( "org.hibernate.validator.internal.util.logging to org.jboss.logging" );
        assertThat( pattern.getKind() ).isEqualTo( PackageNamePattern.Kind.INCLUSIVE );
        assertThat( pattern.getPattern().pattern() ).isEqualTo( Pattern.compile( "org\\.hibernate\\.validator\\.internal\\.util\\.logging" ).pattern() );
        assertThat( pattern.getTargetModules() ).containsExactly( "org.jboss.logging" );
    }

    @Test
    public void excludeWithWildcard() {
        PackageNamePattern pattern = PackageNamePattern.parsePattern( "!org.hibernate.validator.internal*" );
        assertThat( pattern.getKind() ).isEqualTo( PackageNamePattern.Kind.EXCLUSIVE );
        assertThat( pattern.getPattern().pattern() ).isEqualTo( Pattern.compile( "org\\.hibernate\\.validator\\.internal.*" ).pattern() );
        assertThat( pattern.getTargetModules() ).isEmpty();
    }

    @Test
    public void parsePatterns() {
        StringBuilder patterns = new StringBuilder();
        patterns.append( "org.hibernate.validator.internal.util.logging to org.jboss.logging;\n" );
        patterns.append( "!org.hibernate.validator.internal*;\n" );
        patterns.append( "*;\n" );

        List<PackageNamePattern> patternList = PackageNamePattern.parsePatterns( patterns.toString() );
        assertThat( patternList ).hasSize( 3 );

        PackageNamePattern pattern = patternList.get( 0 );
        assertThat( pattern.getKind() ).isEqualTo( PackageNamePattern.Kind.INCLUSIVE );
        assertThat( pattern.getPattern().pattern() ).isEqualTo( Pattern.compile( "org\\.hibernate\\.validator\\.internal\\.util\\.logging" ).pattern() );
        assertThat( pattern.getTargetModules() ).containsExactly( "org.jboss.logging" );

        pattern = patternList.get( 1 );
        assertThat( pattern.getKind() ).isEqualTo( PackageNamePattern.Kind.EXCLUSIVE );
        assertThat( pattern.getPattern().pattern() ).isEqualTo( Pattern.compile( "org\\.hibernate\\.validator\\.internal.*" ).pattern() );
        assertThat( pattern.getTargetModules() ).isEmpty();

        pattern = patternList.get( 2 );
        assertThat( pattern.getKind() ).isEqualTo( PackageNamePattern.Kind.INCLUSIVE );
        assertThat( pattern.getPattern().pattern() ).isEqualTo( Pattern.compile( ".*" ).pattern() );
        assertThat( pattern.getTargetModules() ).isEmpty();
    }
}
