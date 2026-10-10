package com.example.orders;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The executor background work runs on. A bean of its own, so that a test can
 * replace it with one that runs tasks only when the test says so.
 */
@Configuration
public class ExecutorConfiguration {

    /** Delays each task, standing in for a slow report query. */
    @Bean
    Executor reportExecutor() {
        return CompletableFuture.delayedExecutor(1500, TimeUnit.MILLISECONDS,
                Executors.newVirtualThreadPerTaskExecutor());
    }
}
