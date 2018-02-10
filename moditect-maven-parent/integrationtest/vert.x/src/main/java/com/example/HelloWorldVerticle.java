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
package com.example;

import java.util.Optional;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;

public class HelloWorldVerticle extends AbstractVerticle {

    @Override
    public void start(Future<Void> future) {
        vertx.createHttpServer()
            .requestHandler(r -> {
                String name = Optional.ofNullable( r.getParam( "name" ) ).orElse( "nameless stranger" );
                r.response().end ( "Hello, " + name + "!" );
            })
            .listen( 8080, result -> {
                if ( result.succeeded() ) {
                    future.complete();
                }
                else {
                    future.fail(result.cause());
                }
            } );
    }
}
