# ModiTect - Tooling for the Java 9 Module System

The ModiTect project aims at providing productivity tools for working with
the Java 9 module system ("Jigsaw"). Currently it allows to add module
descriptors to existing JAR files. In future versions functionality may be added
to work with tools like jlink, jdeps or jmod under Maven and other dependency
management tools in a comfortable manner.

## Usage

The only functionality available at this time is the addition of Java 9 module
descriptors to existing JAR files. This functionality is exposed through a Maven
plug-in (the core implementation is a separate module, though, so that plug-ins
for other build systems such as Gradle could be written, too) which is used like
this:

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

* Facilitate creation of module descriptors via jdeps based on POM dependencies
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
