/*
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Copyright The original authors
 *
 *  Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package com.example;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SimpleBean {

    @Logged
    public void SimpleCall() {
        System.out.println("******************************************************************\n" +
                "*   Hello World from a Simple Bean in a CDI JPMS compliant App   *\n" +
                "******************************************************************\n");
    }

}
