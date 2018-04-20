/**
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
package org.moditect;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.moditect.commands.AddModuleInfo;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

public class Moditect {

    public static void main(String[] args) throws Exception {
        CliArgs cliArgs = new CliArgs();
        new JCommander( cliArgs, args );

        new AddModuleInfo( null, null, null, null, cliArgs.outputDirecory, cliArgs.jvmVersion, cliArgs.overwriteExistingFiles ).run();
    }

    @Parameters(separators = "=")
    private static class CliArgs {

        @Parameter(
            names = "--module-info",
            required = true,
            description = "Path to the module-info.java descriptor",
            converter = PathConverter.class
        )
        private Path moduleInfo;

        @Parameter(
                names = "--output-directory",
                required = true,
                description = "Path to a directory for storing the modularized JAR",
                converter = PathConverter.class
        )
        private Path outputDirecory;

        @Parameter(
                names = "--jvm-version",
                required = false,
                description = "The JVM version for which to add the module-info.java descriptor"
        )
        private Integer jvmVersion;

        @Parameter(
                names = "--overwrite-existing-files",
                required = false,
                description = "Whether to overwrite existing files or not"
        )
        private boolean overwriteExistingFiles;
    }

    private static class PathConverter implements IStringConverter<Path> {

        @Override
        public Path convert(String value) {
            return Paths.get( value );
        }
    }
}
