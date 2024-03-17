/*
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Copyright The original authors
 *
 *  Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.moditect.internal.command;

import java.io.PrintWriter;

import org.moditect.spi.log.Log;

/**
 * Wraps Moditect {@link Log} with a {@link PrintWriter} in order to pass
 * {@code jdeps} command output to it.
 *
 * @author Aleks Seovic  2022.04.15
 */
public class LogWriter
        extends PrintWriter {
    private static final String ERROR_PREFIX = "Error:";
    private static final String WARNING_PREFIX = "Warning:";

    private final Log log;

    /**
     * Creates a new PrintWriter that will write everything to the specified log
     * in addition to {@code System.out}.
     *
     * @param log the log to write to
     */
    public LogWriter(Log log) {
        super(System.out);
        this.log = log;
    }

    /**
     * Prints a String and then terminates the line.  This method behaves as
     * though it invokes {@link #print(String)} and then {@link #println()}.
     *
     * @param sText the {@code String} value to be printed
     */
    public void println(String sText) {
        if (sText.startsWith(ERROR_PREFIX)) {
            log.error(sText.substring(ERROR_PREFIX.length() + 1));
        }
        else if (sText.startsWith(WARNING_PREFIX)) {
            log.warn(sText.substring(WARNING_PREFIX.length() + 1));
        }
        else {
            log.info(sText);
        }
    }
}
