/*
 *  SPDX-License-Identifier: Apache-2.0
 *
 *   Copyright The original authors
 *
 *   Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.moditect.spi.log;

public interface Log {

    public void debug(CharSequence message);

    public void info(CharSequence message);

    public void warn(CharSequence message);

    public void error(CharSequence message);
}
