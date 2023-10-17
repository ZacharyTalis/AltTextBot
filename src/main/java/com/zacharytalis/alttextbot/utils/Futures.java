package com.zacharytalis.alttextbot.utils;

import com.google.common.collect.Streams;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Futures {
    public static <T> CompletableFuture<T> supplyAsync(Supplier<CompletableFuture<T>> supplier) {
        return Toolbox.nullFuture().thenCompose(_void -> supplier.get());
    }

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

    @SafeVarargs
    public static <T> CompletableFuture<Stream<T>> allOf(CompletableFuture<T>... futures) {
        return allOf(List.of(futures));
    }

    public static <T> CompletableFuture<List<T>> lift(List<CompletableFuture<T>> futures) {
        final var stream = futures.stream();
        return CompletableFuture.supplyAsync(() -> stream.map(CompletableFuture::join).collect(Collectors.toList()));
    }
}
