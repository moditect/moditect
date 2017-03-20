# ModiTect - Tooling for the Java 9 Module System

The ModiTect project aims at providing productivity tools for working with
the Java 9 module system ("Jigsaw").

Currently the following tasks are supported:

* Generating module-info.java descriptors for given artifacts
* Adding module descriptors to existing JAR files
* Creating module runtime images

In future versions functionality may be added to work with other tools like
jmod etc. under Maven and other dependency management tools in a comfortable
manner.

* [Usage](#usage)
   * [Generating module-info.java descriptors](#generating-module-infojava-descriptors)
   * [Adding module descriptors to existing JAR files](#adding-module-descriptors-to-existing-jar-files)
   * [Creating module runtime images](#creating-module-runtime-images)
* [Example](#example)
* [Installation](#installation)
* [Status](#status)
* [Further Planned Features](#further-planned-features)
* [Related Work](#related-work)
* [License](#license)

## Usage

ModiTect's functionality is currently exclusively exposed through a Maven
plug-in. The core implementation is a separate module, though, so that plug-ins
for other build systems such as Gradle could be written, too.

### Generating module-info.java descriptors

To create a module-info.java descriptor for a given artifact, configure the
_generate-module-info_ goal as follows:

```xml
...
<plugin>
    <groupId>org.moditect</groupId>
    <artifactId>moditect-maven-plugin</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <executions>
        <execution>
            <id>generate-module-info</id>
            <phase>generate-sources</phase>
            <goals>
                <goal>generate-module-info</goal>
            </goals>
            <configuration>
                <modules>
                    <module>
                        <artifact>
                            <groupId>com.example</groupId>
                            <artifactId>example-core</artifactId>
                            <version>1.0.0.Final</version>
                        </artifact>
                        <additionalDependencies>
                            <dependency>
                                <groupId>com.example</groupId>
                                <artifactId>example-extended</artifactId>
                                <version>1.0.0.Final</version>
                            </dependency>
                        </additionalDependencies>
                        <moduleName>com.example.core</moduleName>
                        <exportExcludes>
                            <exportExclude>com\.example\.core\.internal\..*</exportExclude>
                        </exportExcludes>
                        <addServiceUses>true</addServiceUses>
                    </module>
                    <module>
                        ...
                    </module>
                </modules>
            </configuration>
        </execution>
    </executions>
</plugin>
...
```

This will generate a module descriptor at _target/generated-sources/com.example.core/module-info.java_.

For each module to be processed, the following configuration options exist:

* `artifact`: The GAV coordinates of the artifact for which a descriptor should
be generated (required)
* `additionalDependencies`: Additional artifacts to be processed; useful if the
main artifact depends on code from another artifact but doesn't declare a
dependency to that one (optional)
* `moduleName`: Name to be used within the descriptor; if not given the name
will be derived from the JAR name as per the naming rules for automatic modules
(optional)
* `exportExcludes`: Regular expressions allowing to filter the list of exported
packages (optional)
* `addServiceUses`: If `true`, the given artifact will be scanned for usages of
`ServiceLoader#load()` and if usages passing a class-literal are found
(`load( MyService.class )`), an equivalent `uses()` clause will be added to the
generated descriptor; usages of `load()` where a non-literal class object is
passed, are ignored (optional, defaults to `false`)

It is also possible to run this goal directly, specifying the different options
as JVM parameters like this:

```
mvn moditect:generate-module-info \
    -Dmoditect.artifact=com.example:example-core:1.0.0.Final \
    -Dmoditect.moduleName=com.example.core \
    -Dmoditect.additionalDependencies=com.example:example-extended:1.0.0.Final \ -Dmoditect.exportExcludes=com\.example\.core\.internal\..* \
    -Dmoditect.addServiceUses=true
```
### Adding module descriptors to existing JAR files

To add a module descriptor for a given artifact, configure the
_generate-module-info_ goal as follows:

```xml
...
<plugin>
    <groupId>org.moditect</groupId>
    <artifactId>moditect-maven-plugin</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <executions>
        <execution>
            <id>add-module-infos</id>
            <phase>generate-resources</phase>
            <goals>
                <goal>add-module-info</goal>
            </goals>
            <configuration>
                <outputDirectory>${project.build.directory}/modules</outputDirectory>
                <modules>
                    <module>
                        <artifact>
                            <groupId>com.example</groupId>
                            <artifactId>example-core</artifactId>
                            <version>1.0.0.Final</version>
                        </artifact>
                        <moduleInfoSource>
                            module com.example.core {
                                requires java.logging;
                                exports com.example.api;
                                provides com.example.api.SomeService
                                    with com.example.internal.SomeServiceImpl;
                            }
                        </moduleInfoSource>
                    </module>
                    <module>
                        ...
                    </module>
                </modules>
            </configuration>
        </execution>
    </executions>
</plugin>
...
```

For each module to be processed, the following configuration options exist:

* `artifact`: The GAV coordinates of the artifact for which a descriptor should
be generated (required)
* `moduleInfoSource`: Inline representation of a module-info.java descriptor
(optional; either this or `moduleInfoFile` must be given)
* `moduleInfoFile`: Path to a module-info.java descriptor
(optional; either this or `moduleInfoSource` must be given)
* `mainClass`: The fully-qualified name of the main class to be added to the
module descriptor (optional)

### Creating module runtime images

To create a modular runtime image (see
[JEP 220](http://openjdk.java.net/jeps/220)), configure the
_create-runtime-image_ goal as follows:

```xml
...
<plugin>
    <groupId>org.moditect</groupId>
    <artifactId>moditect-maven-plugin</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <executions>
        <execution>
            <id>create-runtime-image</id>
            <phase>package</phase>
            <goals>
                <goal>create-runtime-image</goal>
            </goals>
            <configuration>
                <modulePath>
                    <path>${project.build.directory}/modules</path>
                </modulePath>
                <modules>
                    <module>com.example.module1</module>
                    <module>com.example.module2</module>
                </modules>
                <launcher>
                    <name>helloWorld</name>
                    <module>com.example.module1</module>
                </launcher>
                <outputDirectory>
                    ${project.build.directory}/jlink-image
                </outputDirectory>
            </configuration>
        </execution>
    </executions>
</plugin>
...
```
The following configuration options exist:

* `modulePath`: One or more directories with modules to be considered for
creating the image (required); the `jmods` directory of the current JVM will be
added implicitly, so it doesn't have to be given here
* `modules`: The module(s) to be used as the root for resolving the modules to
be added to the image (required)
* `outputDirectory`: Directory in which the runtime image should be created
(required)
* `launcher`: file name and main module for creating a launcher file (optional)

Once the image has been created, it can be executed by running:

```
./<outputDirectory>/bin/java --module com.example
```

Or, if a launcher has been configured:

```
./<outputDirectory>/bin/<launcherName>
```

## Example

The [POM file](integrationtest/undertow/pom.xml) in _integrationtest/undertow_
shows a more complete example. It adds module descriptors for
[Undertow Core](http://undertow.io/) and its dependencies, i.e. it allows to run
the Undertow web server based on Java 9 modules.

Run

    cd integrationtest/undertow
    mvn clean install

to build the example. You then can start Undertow by executing

    java --module-path target/modules --module com.example

Alternatively, you can run the modular runtime image created by the example:

    ./target/jlink-image/bin/helloWorld

Then visit [http://localhost:8080/?name=YourName](http://localhost:8080/?name=YourName)
in your browser for the canonical "Hello World" example.

## Installation

ModiTect is not yet deployed to Maven Central or any other repo server.
Instead clone it from GitHub (https://github.com/moditect/moditect.git)
and build it locally by running `mvn clean install`.

ModiTect is using a non-released version of [ASM](http://asm.ow2.org/).
As this version is not available in Maven Central yet, it has been copied into
this project for the time being. It is based on revision r1843 of
svn://svn.forge.objectweb.org/svnroot/asm/branches/ASM_6_FUTURE/asm.
Once ASM 6 has been released in a version supporting the latest JDK 9 builds,
ModiTect will use that official ASM version.

## Status

ModiTect currently represents the result of one weekend's work. I.e. it's a
very basic proof-of-concept of this point. Not all features of _module-info.java_
files are supported yet and it generally has lots of rough edges. Use it at your
own risk.

## Further Planned Features

Adding module descriptors to existing JARs is the first functionality
implemente in ModiTect. Potential future developments include:

* Update existing module descriptors (e.g. to remove/replace a requires clause)
* Install/Deploy updated (modularized) JARs with a new name/classifier etc.
* Facilitate creation of jlink image directories
* YOUR ideas :)

## Related Work

[ModuleTools](https://github.com/forax/moduletools/) by Remi Forax shows how to
assemble module descriptors using ASM.

## License

ModiTect is licensed under the Apache License version 2.0. ASM is licensed
under the 3 clause BSD license (see the license.txt file in the _asm_
directory).
