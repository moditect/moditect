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
package org.moditect.test.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.regex.Pattern;

import org.junit.Test;
import org.moditect.model.DependencePattern;

public class DependencePatternTest {

    @Test
    public void all() {
        DependencePattern pattern = DependencePattern.parsePattern( "*" );
        assertThat( pattern.getPattern().pattern() ).isEqualTo( Pattern.compile( ".*" ).pattern() );
        assertThat( pattern.getModifiers() ).isEmpty();
    }

    @Test
    public void noModifiers() {
        DependencePattern pattern = DependencePattern.parsePattern( "java.validation" );
        assertThat( pattern.getPattern().pattern() ).isEqualTo( Pattern.compile( "java\\.validation" ).pattern() );
        assertThat( pattern.getModifiers() ).isEmpty();
    }

    @Test
    public void oneModifier() {
        DependencePattern pattern = DependencePattern.parsePattern( "static java.validation" );
        assertThat( pattern.getPattern().pattern() ).isEqualTo( Pattern.compile( "java\\.validation" ).pattern() );
        assertThat( pattern.getModifiers() ).containsOnly( "static" );
    }

    @Test
    public void twoModifiers() {
        DependencePattern pattern = DependencePattern.parsePattern( "static transitive java.validation" );
        assertThat( pattern.getPattern().pattern() ).isEqualTo( Pattern.compile( "java\\.validation" ).pattern() );
        assertThat( pattern.getModifiers() ).containsOnly( "static", "transitive" );
    }
}
