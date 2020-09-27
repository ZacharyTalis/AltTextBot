package com.zacharytalis.alttextbot.utils;

import com.google.common.collect.Streams;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class Futures {
    public static <T> CompletableFuture<T> runThenGet(CompletableFuture<?> run, Supplier<CompletableFuture<T>> supplier) {
        final CompletableFuture<T> future = new CompletableFuture<>();

        run.thenAcceptAsync(unused -> {
            supplier.get().thenAccept(future::complete);
        });

        return future;
    }

    public static <T> CompletableFuture<T> runThenGet(Runnable run, Supplier<CompletableFuture<T>> get) {
        return runThenGet(CompletableFuture.runAsync(run), get);
    }

    public static <T> CompletableFuture<Stream<T>> allOf(Iterable<CompletableFuture<T>> futures) {
        return CompletableFuture.supplyAsync(() -> Streams.stream(futures).map(CompletableFuture::join));
    }

    public static <T> CompletableFuture<Stream<T>> allOf(CompletableFuture<T>... futures) {
        return allOf(List.of(futures));
    }
}
