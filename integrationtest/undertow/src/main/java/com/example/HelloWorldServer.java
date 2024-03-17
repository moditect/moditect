/*
 *  SPDX-License-Identifier: Apache-2.0
 *
 *   Copyright The original authors
 *
 *   Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package com.example;

import java.util.ArrayDeque;
import java.util.Collections;

import io.undertow.Undertow;
import io.undertow.util.Headers;

public class HelloWorldServer {

    public static void main(final String[] args) {
        Undertow server = Undertow.builder()
                .addHttpListener(8080, "0.0.0.0")
                .setHandler(exchange -> {
                    String name = exchange.getQueryParameters()
                            .getOrDefault("name", new ArrayDeque<>(Collections.singleton("nameless stranger")))
                            .getFirst();
                    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                    exchange.getResponseSender().send("Hello, " + name + "!");
                })
                .build();

        server.start();
    }
}
