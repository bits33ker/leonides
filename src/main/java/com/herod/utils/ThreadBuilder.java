package com.herod.utils;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

public class ThreadBuilder {
    public ThreadBuilder() {
    }

    public static ScheduledExecutorService buildNewSingleScheduledExecutorService(String name) {
        ThreadFactory namedThreadFactory = (new ThreadFactoryBuilder()).setNameFormat(name).build();
        return Executors.newScheduledThreadPool(1, namedThreadFactory);
    }

    public static ScheduledExecutorService buildNewSingleScheduledExecutorService(int threadCount, String name) {
        ThreadFactory namedThreadFactory = (new ThreadFactoryBuilder()).setNameFormat(name).build();
        return Executors.newScheduledThreadPool(threadCount, namedThreadFactory);
    }

    public static ExecutorService buildNewSingleExecutorService(String name) {
        ThreadFactory namedThreadFactory = (new ThreadFactoryBuilder()).setNameFormat(name).build();
        return Executors.newSingleThreadExecutor(namedThreadFactory);
    }

    public static ExecutorService buildNewFixedThreadPool(int threadCount, String name) {
        ThreadFactory namedThreadFactory = (new ThreadFactoryBuilder()).setNameFormat(name).build();
        return Executors.newFixedThreadPool(threadCount, namedThreadFactory);
    }

    public static ScheduledExecutorService buildNewSingleScheduledExecutorServicePool(int threadCount, String name) {
        ThreadFactory namedThreadFactory = (new ThreadFactoryBuilder()).setNameFormat(name).build();
        return Executors.newScheduledThreadPool(threadCount, namedThreadFactory);
    }
}
