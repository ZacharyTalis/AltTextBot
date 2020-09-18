package com.zacharytalis.alttextbot.utils;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

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
}
