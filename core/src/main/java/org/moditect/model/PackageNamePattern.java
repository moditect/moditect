/*
 *  SPDX-License-Identifier: Apache-2.0
 *
 *   Copyright The original authors
 *
 *   Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.moditect.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PackageNamePattern {

    public static enum Kind {
        INCLUSIVE,
        EXCLUSIVE;
    }

    private static final Pattern INCLUSIVE_PATTERN = Pattern.compile("(.*?)((\\s*to\\s)(.*))?");
    private static final Pattern EXCLUSIVE_PATTERN = Pattern.compile("(!)(.*?)");
    private static final Pattern MODULES_PATTERN = Pattern.compile("\\s*,\\s*");

    private final Kind kind;
    private final Pattern pattern;
    private final List<String> targetModules;

    public static List<PackageNamePattern> parsePatterns(String patterns) {
        if (patterns == null) {
            return Collections.emptyList();
        }

        return Arrays.stream(patterns.trim().split(";"))
                .map(PackageNamePattern::parsePattern)
                .collect(Collectors.toList());
    }

    public static PackageNamePattern parsePattern(String pattern) {
        pattern = pattern.trim();

        if (pattern.startsWith("!")) {
            Matcher matcher = EXCLUSIVE_PATTERN.matcher(pattern);
            if (!matcher.matches()) {
                throw new IllegalArgumentException("Invalid exclusive pattern: " + pattern);
            }
            else {
                return exclusive(matcher.group(2));
            }
        }
        else {
            Matcher matcher = INCLUSIVE_PATTERN.matcher(pattern);
            if (!matcher.matches()) {
                throw new IllegalArgumentException("Invalid inclusive pattern: " + pattern);
            }
            else {
                return inclusive(matcher.group(1), matcher.group(2) != null ? modules(matcher.group(4)) : Collections.emptyList());
            }
        }
    }

    private static List<String> modules(String modules) {
        return Arrays.asList(MODULES_PATTERN.split(modules));
    }

    private static PackageNamePattern inclusive(String pattern, List<String> targetModules) {
        return new PackageNamePattern(Kind.INCLUSIVE, pattern, targetModules);
    }

    private static PackageNamePattern exclusive(String pattern) {
        return new PackageNamePattern(Kind.EXCLUSIVE, pattern, Collections.emptyList());
    }

    private PackageNamePattern(Kind kind, String pattern, List<String> targetModules) {
        this.kind = kind;
        this.pattern = Pattern.compile(pattern.replace(".", "\\.").replace("*", ".*"));
        this.targetModules = targetModules;
    }

    public boolean matches(String packageName) {
        return pattern.matcher(packageName).matches();
    }

    public Kind getKind() {
        return kind;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public List<String> getTargetModules() {
        return targetModules;
    }

    @Override
    public String toString() {
        return "PackageNamePattern[kind=" + kind + ", pattern=" + pattern + ", targetModules=" + targetModules + "]";
    }
}
