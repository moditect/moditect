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
module moditect.core {
    requires java.base;
    requires java.compiler;
    requires javaparser.core;
    requires org.objectweb.asm;
    requires aether.api;
    requires aether.util;
    requires jcommander;

    exports org.moditect.spi.log;
    exports org.moditect.commands;
    exports org.moditect.internal.analyzer;
    exports org.moditect.internal.command;
    exports org.moditect.internal.compiler;
    exports org.moditect.model.add;
    exports org.moditect.model.common;
    exports org.moditect.model.generate;
    exports org.moditect.model.image;
    exports org.moditect.generator;
    exports org.moditect.dependency;
    exports org.moditect.model;
}
