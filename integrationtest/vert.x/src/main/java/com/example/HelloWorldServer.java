/*
 *  SPDX-License-Identifier: Apache-2.0
 *
 *   Copyright The original authors
 *
 *   Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package com.example;

import io.vertx.core.Vertx;

public class HelloWorldServer {

    public static void main(final String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(HelloWorldVerticle.class.getName());
    }
}
