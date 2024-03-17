/*
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Copyright The original authors
 *
 *  Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.moditect.internal.command;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.moditect.spi.log.Log;

/**
 * Executes a specified external processor, logging output to the given logger.
 *
 * @author Gunnar Morling
 */
public class ProcessExecutor {

    public static void run(String name, List<String> command, Log log) {
        ProcessBuilder builder = new ProcessBuilder(command);

        Process process;
        List<String> outputLines = new ArrayList<>();
        try {
            process = builder.start();

            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                outputLines.add(line);
                log.debug(line);
            }

            BufferedReader err = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = err.readLine()) != null) {
                log.error(line);
            }

            process.waitFor();
        }
        catch (IOException | InterruptedException e) {
            for (String line : outputLines) {
                log.error(line);
            }

            throw new RuntimeException("Couldn't run " + name, e);
        }

        if (process.exitValue() != 0) {
            for (String line : outputLines) {
                log.error(line);
            }

            throw new RuntimeException("Execution of " + name + " failed");
        }
    }
}
