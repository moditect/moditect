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
package org.moditect;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

import org.moditect.commands.AddModuleInfo;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

public class Moditect {

    public static void main(String[] args) throws Exception {
        CliArgs cliArgs = new CliArgs();
        new JCommander( cliArgs, args );

        new AddModuleInfo( null, null, null, null, cliArgs.outputDirecory, cliArgs.jvmVersion, cliArgs.overwriteExistingFiles, cliArgs.timestamp ).run();
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
                description = "The JVM version for which to add the module-info.java descriptor, "
                        + "or \"base\" to add the descriptor to the root of the jarfile (default: 9)"
        )
        private String jvmVersion;

        @Parameter(
                names = "--overwrite-existing-files",
                required = false,
                description = "Whether to overwrite existing files or not"
        )
        private boolean overwriteExistingFiles;

        @Parameter(
            names = "--timestamp",
            required = false,
            description = "Timestamp used when writing archive entries",
            converter = InstantConverter.class
        )
        private Instant timestamp;
    }

    private static class PathConverter implements IStringConverter<Path> {

        @Override
        public Path convert(String value) {
            return Paths.get( value );
        }
    }

    private static class InstantConverter implements IStringConverter<Instant> {

        private static final Instant DATE_MIN = Instant.parse( "1980-01-01T00:00:02Z" );
        private static final Instant DATE_MAX = Instant.parse( "2099-12-31T23:59:59Z" );

        @Override
        public Instant convert(String value) {
            if ( value == null ) {
                return null;
            }

            // Number representing seconds since the epoch
            if ( !value.isEmpty() && isNumeric( value ) ) {
                return Instant.ofEpochSecond( Long.parseLong( value.trim() ) );
            }

            try {
                // Parse the date in UTC such as '2011-12-03T10:15:30Z' or with an offset '2019-10-05T20:37:42+06:00'.
                final Instant date = OffsetDateTime.parse( value )
                    .withOffsetSameInstant( ZoneOffset.UTC ).truncatedTo( ChronoUnit.SECONDS ).toInstant();

                if ( date.isBefore( DATE_MIN ) || date.isAfter( DATE_MAX ) ) {
                    throw new IllegalArgumentException( "'" + date + "' is not within the valid range "
                        + DATE_MIN + " to " + DATE_MAX );
                }
                return date;
            }
            catch ( DateTimeParseException pe ) {
                throw new IllegalArgumentException( "Invalid project.build.outputTimestamp value '" + value + "'",
                    pe );
            }
        }

        private boolean isNumeric( String str ) {
            try {
                Long.parseLong( str.trim() );
                return true;
            } catch( NumberFormatException e ) {
                return false;
            }
        }
    }
}
