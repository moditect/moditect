# ModiTect - Tooling for the Java 9 Module System

Version 1.0.0.Alpha2 - 2017-10-31

The ModiTect project aims at providing productivity tools for working with
the Java 9 module system ("Jigsaw").

Currently the following tasks are supported:

* Generating module-info.java descriptors for given artifacts (Maven dependencies or local JAR
  files)
* Adding module descriptors to your project's JAR as well as existing JAR files (dependencies)
* Creating module runtime images

Compared to authoring module descriptors by hand, using ModiTect saves you work by defining
dependence clauses based on your project's dependencies, describing exported and opened
packages with patterns (instead of listing all packages separately), auto-detecting service
usages and more. You also can use ModiTect to add a module descriptor to your project JAR
while staying on Java 8 with your own build.

In future versions functionality may be added to work with other tools like
jmod etc. under Maven and other dependency management tools in a comfortable
manner.

* [Usage](#usage)
   * [Generating module-info.java descriptors](#generating-module-infojava-descriptors)
   * [Adding a module descriptor to the project JAR](#adding-a-module-descriptor-to-the-project-jar)
   * [Adding module descriptors to existing JAR files](#adding-module-descriptors-to-existing-jar-files)
   * [Creating module runtime images](#creating-module-runtime-images)
* [Example](#example)
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
    <version>1.0.0.Alpha2</version>
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
                        <moduleInfo>
                            <name>com.example.core</name>
                            <exports>
                                !com.example.core.internal*;
                                *;
                            </exports>
                            <requires>
                                static com.some.optional.dependency;
                                *;
                            </requires>
                            <uses>
                                 com.example.SomeService;
                            </uses>
                            <addServiceUses>true</addServiceUses>
                        </moduleInfo>
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
* `moduleInfo`: Allows fine-grained configuration of the generated module
descriptor (optional); has the following sub-elements:
  - `name`: Name to be used within the descriptor; if not given the name
will be derived from the JAR name as per the naming rules for automatic modules
(optional)
  - `open`: Whether the descriptor should be an open module or not (optional, defaults
to `false`)
  - `exports`: List of name patterns for describing the exported packages of the module,
separated by ";". Patterns can be inclusive or exclusive (starting with "!") and may
contain the "\*" as a wildcard. Inclusive patterns may be qualified exports ("to xyz").
For each package from the module, the given patterns are processed in the order they
are given. As soon a package is matched by an inclusive pattern, the package will be
added to the list of exported packages and no further patterns will be applied. As soon
as a package is matched by an exclusive pattern, this package will not be added to the
list of exported packages and no further patterns will be applied.
(optional; the default value is "\*;", i.e. all packages will be exported)
  - `opens`: List of name patterns for describing the open packages of the module,
separated by ";". Patterns can be inclusive or exclusive (starting with "!") and may
contain the "\*" as a wildcard. Inclusive patterns may be qualified exports ("to xyz").
For each package from the module, the given patterns are processed in the order they
are given. As soon a package is matched by an inclusive pattern, the package will be
added to the list of open packages and no further patterns will be applied. As soon
as a package is matched by an exclusive pattern, this package will not be added to the
list of open packages and no further patterns will be applied.
(optional; the default value is "!\*;", i.e. no packages will be opened)
  - `requires`: List of name patterns for describing the dependences of the module,
  based on the automatically determined dependences.
Patterns are inclusive and may contain the "\*" character as a wildcard. Patterns may
contain the `static` and `transitive` modifiers, in which case those modifiers will
override the modifiers of the automatically determined dependence. For each of the
automatically determined dependences of the module, the given patterns are processed in the order they are given. As soon a dependence is matched by a pattern, the dependence will be
added to the list of dependences and no further patterns will be applied. Usually,
only a few dependences will be given explicitly in order to override their modifiers,
followed by a `*;` pattern to add all remaining automatically determined dependences.
  - `addServiceUses`: If `true`, the given artifact will be scanned for usages of
`ServiceLoader#load()` and if usages passing a class literal are found
(`load( MyService.class )`), an equivalent `uses()` clause will be added to the
generated descriptor; usages of `load()` where a non-literal class object is
passed, are ignored (optional, defaults to `false`)
  - `uses`: List of names of used services, separated by ";" only required if `addServiceUses`
cannot be used due to dynamic invocations of `ServiceLoader#load()`, i.e. no class literal is
passed (optional)

It is also possible to run this goal directly, specifying the different options
as JVM parameters like this:

```
mvn moditect:generate-module-info \
    -Dmoditect.artifact=com.example:example-core:1.0.0.Final \
    -Dmoditect.moduleName=com.example.core \
    -Dmoditect.additionalDependencies=com.example:example-extended:1.0.0.Final \ -Dmoditect.exportExcludes=com\.example\.core\.internal\..* \
    -Dmoditect.addServiceUses=true
```

### Adding a module descriptor to the project JAR

To add a module descriptor to the JAR produced by the current Maven project, configure
the _add-module-info_ goal as follows:

```xml
...
<plugin>
    <groupId>org.moditect</groupId>
    <artifactId>moditect-maven-plugin</artifactId>
    <version>1.0.0.Alpha2</version>
    <executions>
        <execution>
            <id>add-module-infos</id>
            <phase>package</phase>
            <goals>
                <goal>add-module-info</goal>
            </goals>
            <configuration>
                <module>
                    <moduleInfo>
                        <name>com.example</name>
                        <exports>
                            !com.example.internal.*;
                            *;
                        </exports>
                    </moduleInfo>
                </module>
            </configuration>
        </execution>
    </executions>
</plugin>
...
```

The following configuration options exist for the `<module>` configuration element:

* `moduleInfoSource`: Inline representation of a module-info.java descriptor
(optional; either this or `moduleInfoFile` or `moduleInfo` must be given)
* `moduleInfoFile`: Path to a module-info.java descriptor
(optional; either this or `moduleInfoSource` or `moduleInfo` must be given)
* `moduleInfo`: A `moduleInfo` configuration as used with the `generate-module-info`
goal (optional; either this or `moduleInfoSource` or `moduleInfoFile` must be given)
* `mainClass`: The fully-qualified name of the main class to be added to the
module descriptor (optional)

Note that `moduleInfoSource` and `moduleInfoFile` can be used on Java 8, allowing to add
a Java 9 module descriptor to your JAR also if you did not move to Java 9 for your own
build yet. `moduleInfo` can only be used on Java 9 or later.

### Adding module descriptors to existing JAR files

To add a module descriptor for a given dependency, configure the
_add-module-info_ goal as follows:

```xml
...
<plugin>
    <groupId>org.moditect</groupId>
    <artifactId>moditect-maven-plugin</artifactId>
    <version>1.0.0.Alpha2</version>
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
be generated (either this or `file` must be given)
* `file`: Path to the file for which a descriptor should be generated (either
  this or `artifact` must be given)
* `moduleInfoSource`: Inline representation of a module-info.java descriptor
(optional; either this or `moduleInfoFile` or `moduleInfo` must be given)
* `moduleInfoFile`: Path to a module-info.java descriptor
(optional; either this or `moduleInfoSource` or `moduleInfo` must be given)
* `moduleInfo`: A `moduleInfo` configuration as used with the `generate-module-info`
goal (optional; either this or `moduleInfoSource` or `moduleInfoFile` must be given)
* `mainClass`: The fully-qualified name of the main class to be added to the
module descriptor (optional)
* `version`: The version to be added to the module descriptor; if not given and
`artifact` is given, the artifact's version will be used; otherwise no version
will be added (optional)

The modularized JARs can be found in the folder given via `outputDirectory`.

### Creating modular runtime images

To create a modular runtime image (see
[JEP 220](http://openjdk.java.net/jeps/220)), configure the
_create-runtime-image_ goal as follows:

```xml
...
<plugin>
    <groupId>org.moditect</groupId>
    <artifactId>moditect-maven-plugin</artifactId>
    <version>1.0.0.Alpha2</version>
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
* `stripDebug` whether to strip debug symbols or not (optional, defaults to `false`)

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

## Status

ModiTect is at a very early stage of development and it still has some rough edges.
Use it at your own risk.

## Further Planned Features

Adding module descriptors to existing JARs is the first functionality
implemented in ModiTect. Potential future developments include:

* Update existing module descriptors (e.g. to remove/replace a requires clause)
* Better support for generating and adding a module descriptor to the JAR produced by a
  build itself (i.e. not a JAR it depends on)
* Install/Deploy updated (modularized) JARs with a new name/classifier etc.
* Adding transitive modifier to dependences based on whether their types are exposed in a
  module's exported API or not
* YOUR ideas :)

## Related Work

[ModuleTools](https://github.com/forax/moduletools/) by Remi Forax shows how to
assemble module descriptors using ASM.

## License

ModiTect is licensed under the Apache License version 2.0. ASM (which is contained within
the ModiTect JAR in the `org/moditect/internal/shaded/asm/` package) is licensed under the
3 clause BSD license (see _etc/LICENSE_ASM.txt_).
