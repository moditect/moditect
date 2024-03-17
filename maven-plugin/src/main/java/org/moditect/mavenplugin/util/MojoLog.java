/*
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Copyright The original authors
 *
 *  Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.moditect.mavenplugin.util;

import org.moditect.spi.log.Log;

/**
 * Log delegating to the Maven logging API.
 *
 * @author Gunnar Morling
 */
public class MojoLog implements Log {

    private final org.apache.maven.plugin.logging.Log delegate;

    public MojoLog(org.apache.maven.plugin.logging.Log delegate) {
        this.delegate = delegate;
    }

    @Override
    public void debug(CharSequence message) {
        delegate.debug(message);
    }

    @Override
    public void info(CharSequence message) {
        delegate.info(message);
    }

    @Override
    public void warn(CharSequence message) {
        delegate.warn(message);
    }

    @Override
    public void error(CharSequence message) {
        delegate.error(message);
    }
}
