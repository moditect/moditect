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
