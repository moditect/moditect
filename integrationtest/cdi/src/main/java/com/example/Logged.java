package com.example;

import java.lang.annotation.Retention;

import jakarta.interceptor.InterceptorBinding;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@InterceptorBinding
@Retention(RUNTIME)
public @interface Logged {
}
