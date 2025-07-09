/*
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Copyright The original authors
 *
 *  Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package com.example;

import jakarta.enterprise.inject.se.SeContainerInitializer;

public class MyApplication {

    public static void main(String[] args) {
        var initializer = SeContainerInitializer.newInstance();
        initializer.addBeanClasses(SimpleBean.class, LogInterceptor.class, LoggerProducer.class);

        var container = initializer.initialize();
        var myBean = container.select(SimpleBean.class).get();
        myBean.SimpleCall();
        container.close();
    }
}
