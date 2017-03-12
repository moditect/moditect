# ModiTect - Tooling for the Java 9 Module System

The ModiTect project aims at providing productivity tools for working with
the Java 9 module system ("Jigsaw"). Currently it supports the following two
tasks:

* Generating module-info.java descriptors for given artifacts
* Adding module descriptors to existing JAR files

In future versions functionality may be added to work with tools like jlink,
jmod etc. under Maven and other dependency management tools in a comfortable
manner.

## Usage

ModiTect's functionality is currently exclusively exposed through a Maven
plug-in. The core implementation is a separate module, though, so that plug-ins
for other build systems such as Gradle could be written, too.

## Generating module-info.java descriptors

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

## Adding module descriptors to existing JAR files

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

## Example

The [POM file](integrationtest/pom.xml) in _integrationtest_ shows a more
complete example. It adds module descriptors for [Undertow Core](http://undertow.io/)
and its dependencies, i.e. it allows to run the Undertow web server based on
Java 9 modules.

Run

    mvn clean install -pl integrationtest

to build the example. You then can start Undertow by executing

    java --module-path integrationtest/target/modules --module com.example

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
