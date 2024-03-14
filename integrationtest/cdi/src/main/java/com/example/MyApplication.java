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
