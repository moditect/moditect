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
package org.moditect.mavenplugin.image.model;

/**
 * A jlink launcher configuration.
 *
 * @author Gunnar Morling
 */
public class Launcher {

    private String mainClass;
    private String module;
    private String name;

    public String getMainClass() {
      return mainClass;
    }

    public String getModule() {
        return module;
    }

    public String getName() {
        return name;
    }

    public void setMainClass(String mainClass) {
      this.mainClass = mainClass;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public void setName(String name) {
        this.name = name;
    }
}
