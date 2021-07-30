/*
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

import java.util.ArrayDeque;
import java.util.Collections;

import io.undertow.Undertow;
import io.undertow.util.Headers;

public class HelloWorldServer {

    public static void main(final String[] args) {
        Undertow server = Undertow.builder()
            .addHttpListener( 8080, "0.0.0.0" )
            .setHandler( exchange -> {
                String name = exchange.getQueryParameters()
                    .getOrDefault( "name", new ArrayDeque<>( Collections.singleton( "nameless stranger" ) ) )
                    .getFirst();
                exchange.getResponseHeaders().put( Headers.CONTENT_TYPE, "text/plain" );
                exchange.getResponseSender().send( "Hello, " + name + "!" );
            } )
            .build();

        server.start();
    }
}
