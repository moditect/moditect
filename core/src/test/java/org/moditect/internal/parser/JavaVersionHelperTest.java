/*
 *  SPDX-License-Identifier: Apache-2.0
 *
 *   Copyright The original authors
 *
 *   Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.moditect.internal.parser;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class JavaVersionHelperTest {

    private JavaVersionHelper testTarget = new JavaVersionHelper();

    @Test
    public void current() {
        JavaVersionHelper.Version version = testTarget.javaVersion();
        assertThat(version.major()).isPositive();
        assertThat(version.minor()).isNotNull();
        assertThat(version.mini()).isNotNull();
    }

    @Test
    public void simple() {
        JavaVersionHelper.Version version = testTarget.javaVersion("11.0.10");
        assertThat(version.major()).isEqualTo(11);
        assertThat(version.minor()).isEqualTo(0);
        assertThat(version.mini()).isEqualTo(10);
    }

    @Test
    public void incomplete() {
        JavaVersionHelper.Version version = testTarget.javaVersion("11");
        assertThat(version).isNull();
    }

    @Test
    public void oldStyle() {
        JavaVersionHelper.Version version = testTarget.javaVersion("1.6.0_23");
        assertThat(version.major()).isEqualTo(1);
        assertThat(version.minor()).isEqualTo(6);
        assertThat(version.mini()).isZero();
    }

    @Test
    public void otherStuff() {
        JavaVersionHelper.Version version = testTarget.javaVersion("1.6.0_23-otherStuff");
        assertThat(version.major()).isEqualTo(1);
        assertThat(version.minor()).isEqualTo(6);
        assertThat(version.mini()).isZero();
    }
}
