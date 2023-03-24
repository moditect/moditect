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
