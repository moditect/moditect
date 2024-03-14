package com.example;

import org.jboss.logging.Logger;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Dependent
@Interceptor
@Logged
@Priority(Interceptor.Priority.LIBRARY_BEFORE)
public class LogInterceptor {

    @Inject
    Logger logger;

    @AroundInvoke
    Object timeMethod(InvocationContext context) throws Exception {
        logger.info("*** before calling method " + context.getMethod().getName() + " ***\n");
        try {
            return context.proceed();
        }
        catch (Exception e) {
            throw e;
        }
        finally {
            logger.info("=== after calling method " + context.getMethod().getName() + " ===");
        }

    }
}
