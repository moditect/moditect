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
package org.moditect.commands;

import org.moditect.internal.command.LogWriter;
import org.moditect.model.Version;
import org.moditect.spi.log.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;

public class GenerateModuleList {

	private final Path projectJar;
	private final Set<Path> dependencies;
	private final Version jvmVersion;
	private final Log log;

	private final ToolProvider jdeps;

	public GenerateModuleList(Path projectJar, Set<Path> dependencies, Version jvmVersion, Log log) {
		this.projectJar = projectJar;
		this.dependencies = dependencies;
		this.jvmVersion = jvmVersion;
		this.log = log;

		this.jdeps = ToolProvider
				.findFirst( "jdeps" )
				.orElseThrow(() -> new RuntimeException("jdeps tool not found"));
	}

	public void run() {
		ByteArrayOutputStream outStream    = new ByteArrayOutputStream();
		PrintStream out = new PrintStream(outStream);
		jdeps.run(out , System.err, "--version");
		out.close();
		int jdepsVersion = Runtime.Version
				.parse(outStream.toString().strip())
				.feature();
		if (jdepsVersion < 12) {
			log.error("The jdeps option this plugin uses to list JDK modules only works flawlessly on JDK 12+, so please use that to run this goal.");
			return;
		}

		List<String> command = new ArrayList<>();
		command.add("--print-module-deps");
		command.add("--ignore-missing-deps");
		command.add("--multi-release");
		command.add(String.valueOf(jvmVersion.feature()));
		command.add("--class-path");
		String classPath = dependencies.stream()
				.map(Path::toAbsolutePath)
				.map(Path::toString)
				.collect(Collectors.joining(File.pathSeparator));
		command.add(classPath);
		command.add(projectJar.toAbsolutePath().toString());

		log.debug( "Running jdeps " + String.join( " ", command ) );

		LogWriter logWriter = new LogWriter(log);
		int result = jdeps.run( logWriter, logWriter, command.toArray( new String[0] ) );
		if (result != 0) {
			throw new IllegalStateException("Invocation of jdeps failed: jdeps " + String.join(  " ", command ) );
		}
	}

}
