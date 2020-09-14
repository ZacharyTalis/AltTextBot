package com.zacharytalis.alttextbot.concurrent;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicInteger;

public class BotThreadFactory implements ForkJoinWorkerThreadFactory {
    private static final ForkJoinWorkerThreadFactory wrapped = ForkJoinPool.defaultForkJoinWorkerThreadFactory;

    private static final AtomicInteger counter = new AtomicInteger(1);

    @Override
    public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
        final var thread = wrapped.newThread(pool);
        final var id = counter.getAndIncrement();
        thread.setName(String.format("Bot Worker %d", id));
        return thread;
    }
}
