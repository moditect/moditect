/*
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Copyright The original authors
 *
 *  Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package com.example;

import java.util.Optional;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;

public class HelloWorldVerticle extends AbstractVerticle {

    @Override
    public void start(Future<Void> future) {
        vertx.createHttpServer()
                .requestHandler(r -> {
                    String name = Optional.ofNullable(r.getParam("name")).orElse("nameless stranger");
                    r.response().end("Hello, " + name + "!");
                })
                .listen(8080, result -> {
                    if (result.succeeded()) {
                        future.complete();
                    }
                    else {
                        future.fail(result.cause());
                    }
                });
    }
}
