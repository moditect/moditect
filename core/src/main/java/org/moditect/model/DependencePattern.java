/*
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A pattern for matching dependences (requires directives). Any matching
 * dependence will be amended with the pattern's modifiers or it will be
 * excluded, if the pattern isn't inclusive.
 *
 * @author Gunnar Morling
 */
public class DependencePattern {

    private static final Pattern PATTERN = Pattern.compile( "((.*)\\s+)?(.*?)" );

    private final boolean inclusive;
    private final Pattern pattern;
    private final Set<String> modifiers;

    public static List<DependencePattern> parsePatterns(String patterns) {
        if ( patterns == null ) {
            return Collections.emptyList();
        }

        return Arrays.stream( patterns.trim().split(";") )
            .map( DependencePattern::parsePattern )
            .collect( Collectors.toList() );
    }

    public static DependencePattern parsePattern(String pattern) {
        pattern = pattern.trim();

        Matcher matcher = PATTERN.matcher( pattern );
        if ( !matcher.matches() ) {
            throw new IllegalArgumentException( "Invalid dependence pattern: " + pattern );
        }
        else {
            if ( matcher.group( 3 ) != null ) {
                return new DependencePattern( matcher.group( 3 ), matcher.group( 2 ) );
            }
            else {
                return new DependencePattern( matcher.group( 2 ), null );
            }
        }
    }

    private DependencePattern(String pattern, String modifiers) {
        if (pattern.startsWith("!")) {
            pattern = pattern.substring(1);
            this.inclusive = false;
        }
        else {
            this.inclusive = true;
        }

        this.pattern = Pattern.compile( pattern.replace( ".", "\\." ).replace( "*", ".*" ) );

        if ( modifiers == null ) {
            this.modifiers = Collections.emptySet();
        }
        else {
            this.modifiers = Arrays.stream( modifiers.split( "\\s" ) )
                .map( String::trim )
                .collect( Collectors.toSet() );
        }
    }

    public boolean matches(String packageName ) {
        return pattern.matcher( packageName ).matches();
    }

    public Pattern getPattern() {
        return pattern;
    }

    public Set<String> getModifiers() {
        return modifiers;
    }

    public boolean isMatchAll() {
        return ".*".equals( pattern.pattern() );
    }

    public boolean isInclusive() {
        return inclusive;
    }

    @Override
    public String toString() {
        return "DependencePattern[pattern=" + pattern + ",modifiers=" + modifiers + "]";
    }
}
