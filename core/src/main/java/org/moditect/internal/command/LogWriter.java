/*
 *  Copyright 2017 - 2023 The ModiTect authors
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
    private static final String ERROR_PREFIX   = "Error:";
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
