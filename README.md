# ModiTect - Tooling for the Java Module System

Version 1.0.0.RC2 - 2021-10-04

The ModiTect project aims at providing productivity tools for working with
the Java module system ("Jigsaw").

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
   * [Creating modular runtime images](#creating-modular-runtime-images)
* [Example](#examples)
   * [Undertow](#undertow)
   * [Vert.x](#vertx)
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
    <version>1.0.0.RC2</version>
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
                                !com.excluded.dependency;
                                *;
                            </requires>
                            <opens>
                                com.example.core.internal.controller to javafx.fxml;
                            </opens>
                            <opensResources>
                                com.example.resource;
                                com.example.resource.icon;
                                com.example.resource.sound;
                            </opensResources>
                            <uses>
                                 com.example.SomeService;
                            </uses>
                            <provides>
                                com.example.SomeService with com.example.SomeServiceImpl1,com.example.SomeServiceImpl2;
                            </provides>
                            <addServiceUses>true</addServiceUses>
                        </moduleInfo>
                    </module>
                    <module>
                        ...
                    </module>
                </modules>
                <jdepsExtraArgs>
                  <arg>--upgrade-module-path=...</arg>
                </jdepsExtraArgs>
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
  - `opensResources`: List of package names only containing resources as png, css or mp3 files
i.e. everything not being java files, separated by ";". For JavaFX 9+, it is required to force
open resource-only packages. Please, refer to https://github.com/javafxports/openjdk-jfx/issues/441
for more details. `opensResources` allows to do this. Contrary to `opens`, `opensResources`
cannot accept patterns because Moditect manages patterns using a technical solution that can
only works with compiled java classes. Hence, each resource-only packages must be declared one by one.
  - `requires`: List of name patterns for describing the dependences of the module,
  based on the automatically determined dependences.
Patterns are inclusive or exclusive (starting with "!") and may contain the "\*" character as a wildcard.
Inclusive patterns may
contain the `static` and `transitive` modifiers, in which case those modifiers will
override the modifiers of the automatically determined dependence. For each of the
automatically determined dependences of the module, the given patterns are processed in the order they are given.
As soon as a dependence is matched by a pattern, the dependence will be
added to the list of dependences (if the pattern is inclusive) or the dependence will be
filtered out (for exclusive patterns) and no further patterns will be applied. Usually,
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
  - `provides`: List of services with their provided service implementations, separated by ";". A service and its implementation must be separated by the keyword "with" e.g. `serviceX with implementationX; serviceY with implementationY;`. If the module implements a particular service through several service implementations, those implementation classes must be separated by "," e.g. `myService with implementation1, implementation2, implementation3;`.
  - `jdepsExtraArgs`: A list of arguments passed to the _jdeps_ invocation for creating a "candidate descriptor"

It is also possible to run this goal directly, specifying the different options
as JVM parameters like this:

```
mvn moditect:generate-module-info \
    -Dmoditect.artifact=com.example:example-core:1.0.0.Final \
    -Dmoditect.moduleName=com.example.core \
    -Dmoditect.additionalDependencies=com.example:example-extended:1.0.0.Final \
    -Dmoditect.exportExcludes=com\.example\.core\.internal\..* \
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
    <version>1.0.0.RC2</version>
    <executions>
        <execution>
            <id>add-module-infos</id>
            <phase>package</phase>
            <goals>
                <goal>add-module-info</goal>
            </goals>
            <configuration>
                <jvmVersion>11</jvmVersion>
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

The optional `jvmVersion` element allows to define which JVM version the module descriptor should target
(leveraging the concept of multi-release JARs).
When defined, the module descriptor will be put into `META-INF/versions/${jvmVersion}`.
The value must be `9` or greater.
The special value `base` (the default) can be used to add the descriptor to the root of the final JAR.
Putting the descriptor under `META-INF/versions` can help to increase compatibility with older libraries scanning class files that may fail when encountering the `module-info.class` file
(as chances are lower that such tool will look for class files under `META-INF/versions/...`).

The `jdepsExtraArgs` option can be used to specify a list of arguments passed to the _jdeps_ invocation for creating a "candidate descriptor".

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
    <version>1.0.0.RC2</version>
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
The `jdepsExtraArgs` option can be used to specify a list of arguments passed to the _jdeps_ invocation for creating a "candidate descriptor".

### Creating modular runtime images

To create a modular runtime image (see
[JEP 220](http://openjdk.java.net/jeps/220)), configure the
_create-runtime-image_ goal as follows:

```xml
...
<plugin>
    <groupId>org.moditect</groupId>
    <artifactId>moditect-maven-plugin</artifactId>
    <version>1.0.0.RC2</version>
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
                <excludedResources>
                    <pattern>glob:/com.example/**</pattern>
                </excludedResources>
                <baseJdk>version=9,vendor=openjdk,platform=linux-x64</baseJdk>
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
* `excludedResources` list of patterns for excluding matching resources from the created
runtime image
* `baseJdk`: requirements for identifying a JDK in _~/.m2/toolchains.xml_ whose jmod files
will be used when creating the runtime image (optional; if not given the JDK running the
current build will be used). Must unambiguously identify one toolchain entry of type `jdk`
that matches all given requirements in its `<provides>` configuration. This can be used for
creating runtime images on one platform (e.g. OS X) while targeting another (e.g. Linux).
* `ignoreSigningInformation`: Suppresses a fatal error when signed modular JARs are linked
in the runtime image. The signature-related files of the signed modular JARs aren’t copied
to the runtime image.
* `noManPages`: No man pages will be added
* `noHeaderFiles`: No native header files will be added
* `bindServices`: Link service provider modules and their dependencies
* `jarInclusionPolicy`: Whether to add the application JAR and optionally its dependencies
to the runtime image, under the _jars_ directory, allowing to run a classpath-based
application on a modular runtime image; allowed values are `NONE`, `APP`, and `APP_WITH_DEPENDENCIES`.

In order to identify the JDK images which should go into a custom runtime image for a classpath-based application,
you can run the following goal:

```
list-application-image-modules
```

This will run the _jdeps_ command under the hood, listing all JDK modules required by the application and its dependencies.

Once the image has been created, it can be executed by running:

```
./<outputDirectory>/bin/java --module com.example
```

Or, if a launcher has been configured:

```
./<outputDirectory>/bin/<launcherName>
```

## Examples

### Undertow

The [POM file](integrationtest/undertow/pom.xml) in _integrationtest/undertow_
shows a more complete example. It adds module descriptors for
[Undertow Core](http://undertow.io/) and its dependencies, i.e. it allows to run
the Undertow web server based on Java 9 modules.

Run
```
cd integrationtest/undertow
mvn clean install
```
to build the example. You then can start Undertow by executing

```
java --module-path target/modules --module com.example
```

Alternatively, you can run the modular runtime image created by the example:

```
./target/jlink-image/bin/helloWorld
```

Then visit [http://localhost:8080/?name=YourName](http://localhost:8080/?name=YourName)
in your browser for the canonical "Hello World" example.

### Vert.x

The [POM file](integrationtest/vert.x/pom.xml) in _integrationtest/vertx_
shows a more complete example. It adds module descriptors for
[Vert.x](http://vertx.io) and its dependencies (Netty, Jackson) and creates a modular runtime
image with a "hello world" verticle.

Execute

```
cd integrationtest/vert.x
mvn clean install -Pjlink
```

to build the example.

You can then run the modular runtime image like so:

```
./target/jlink-image/bin/helloWorld
```

Then visit [http://localhost:8080/?name=YourName](http://localhost:8080/?name=YourName)
in your browser for the canonical "Hello World" example.

The runtime image has a size of 45 MB, which could be further improved by a few adjustments to the involved libraries.
E.g. _jackson-databind_ pulls in _java.sql_ unconditionally which could be avoided by making data converters related to
`java.sql` types an optional feature.

#### Using Docker

The Vert.x example can also be run on Docker. To do so, run the build with the "docker-base" profile:

```
mvn clean install -Pdocker-base
```

This will create an image named _moditect/vertx-helloworld-base_ which contains the jlink image.
To run that image execute

```
docker run --rm -t -i -p 8080:8080 moditect/vertx-helloworld-base
```

Changes to the application will require to rebuild the entire `jlink` image which actually isn't needed if just the app itself
changed but not its dependencies (used JDK modules or 3rd-party modules).
Therefore another image can be build using the "docker" profile:

```
mvn clean install -Pdocker
```

This will create an image named _moditect/vertx-helloworld_ which extends the base image and just adds the application module (_com.example_) on the upgrade module path.
Hence that image is very quick to be built (and distributed) once the base image is in place.
To run that image execute

```
docker run --rm -t -i -p 8080:8080 moditect/vertx-helloworld
```

## Status

ModiTect is at an early stage of development and it still has some rough edges.
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
