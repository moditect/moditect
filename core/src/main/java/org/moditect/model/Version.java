/*
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Copyright The original authors
 *
 *  Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.moditect.model;

import java.util.Objects;

/**
 * Simplified version of Runtime.Version (which requires Java 9+).
 */
public class Version {

    private final int feature;

    private Version(int feature) {
        this.feature = feature;
    }

    public static Version valueOf(int feature) {
        return new Version(feature);
    }

    public static Version valueOf(Object feature) {
        return new Version(Integer.parseInt(String.valueOf(feature)));
    }

    public int feature() {
        return feature;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Version))
            return false;
        Version version = (Version) o;
        return feature == version.feature;
    }

    @Override
    public int hashCode() {
        return Objects.hash(feature);
    }

    @Override
    public String toString() {
        return "Version " + feature;
    }

}
