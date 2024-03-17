/*
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Copyright The original authors
 *
 *  Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.moditect.mavenplugin.common.model;

/**
 * @author Gunnar Morling
 */
public class ModuleInfoConfiguration {

    private String requires = "*;";
    private String exports = "*;";
    private String opens = "!*;";
    private String opensResources;
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

    public String getOpensResources() {
        return opensResources;
    }

    public void setOpensResources(String opensResources) {
        this.opensResources = opensResources;
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
        return "ModuleInfoConfiguration [requires=" + requires + ", exports=" + exports + ", opens=" + opens + ", opensResources=" + opensResources
                + ", uses=" + uses + ", provides=" + provides + ", name=" + name + ", addServiceUses=" + addServiceUses + ", open=" + open + "]";
    }
}
