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
package org.moditect.mavenplugin.common.model;

/**
 * @author Gunnar Morling
 */
public class ModuleInfoConfiguration {

    private String requires = "*;";
    private String exports = "*;";
    private String opens = "!*;";
    private String uses;
    private String provides;
    private String name;
    private boolean addServiceUses;
    private boolean open;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRequires() {
        return requires;
    }

    public void setRequires(String requires) {
        this.requires = requires;
    }

    public String getExports() {
        return exports;
    }

    public void setExports(String exports) {
        this.exports = exports;
    }

    public String getOpens() {
        return opens;
    }

    public void setOpens(String opens) {
        this.opens = opens;
    }

    public String getUses() {
        return uses;
    }

    public void setUses(String uses) {
        this.uses = uses;
    }

    public String getProvides() {
        return provides;
    }

    public void setProvides(String provides) {
        this.provides = provides;
    }

    public void setAddServiceUses(boolean addServiceUses) {
        this.addServiceUses = addServiceUses;
    }

    public boolean isAddServiceUses() {
        return addServiceUses;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public boolean isOpen() {
        return open;
    }

    @Override
    public String toString() {
        return "ModuleInfoConfiguration [requires=" + requires + ", exports=" + exports + ", opens=" + opens + ", uses="
                + uses + ", provides=" + provides + ", name=" + name + ", addServiceUses=" + addServiceUses + ", open=" + open + "]";
    }
}
