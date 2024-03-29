package com.zacharytalis.alttextbot.utils;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Sets;
import com.zacharytalis.alttextbot.logging.LoggableLogger;
import com.zacharytalis.alttextbot.logging.Logger;
import com.zacharytalis.alttextbot.logging.PrefixingLogger;
import com.zacharytalis.alttextbot.utils.functions.Runnables;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class Toolbox {
    public record Caller(Class<?> callerClass) {
        public static class InferenceException extends RuntimeException {
            InferenceException() {
                super("Failed to perform caller inference");
            }
        }

        private static final Set<Class<?>> EXCLUDE = Set.of(Toolbox.class, Caller.class);

        private static Caller infer(Class<?>... excluding) throws InferenceException {
            final var irrelevantClasses = exclusions(excluding);
            final var isRelevant = Predicate.<Class<?>>not(irrelevantClasses::contains);

            final var walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
            final var callingClass =
                walker.walk(
                    s -> s.map(StackWalker.StackFrame::getDeclaringClass).filter(isRelevant).findFirst()
                );

            return callingClass.map(Caller::new).orElseThrow(InferenceException::new);
        }

        public static Caller inferExcludingSelf() throws InferenceException {
            final var self = infer().callerClass();
            return infer(self);
        }

        private static Set<Class<?>> exclusions(Class<?>... excluding) {
            final var excl = Sets.newHashSet(excluding);
            excl.addAll(EXCLUDE);

            return Collections.unmodifiableSet(excl);
        }
    }

    public static abstract class PerEnvRunner {
        private static final Supplier<PerEnvRunner> runnerFactory =
            Ref.currentEnv().isProduction() ? ProdRunner::new : TestingRunner::new;

        public static PerEnvRunner getInstance() {
            return runnerFactory.get();
        }

        private static class ProdRunner extends PerEnvRunner {
            @Override
            public PerEnvRunner inTesting(Runnable run) {
                // Do nothing in Testing
                return this;
            }

            @Override
            public PerEnvRunner inProduction(Runnable run) {
                run.run();
                return this;
            }
        }

        private static class TestingRunner extends PerEnvRunner {
            @Override
            public PerEnvRunner inTesting(Runnable run) {
                run.run();
                return this;
            }

            @Override
            public PerEnvRunner inProduction(Runnable run) {
                // Do nothing in Production
                return this;
            }
        }

        public abstract PerEnvRunner inTesting(Runnable run);

        public abstract PerEnvRunner inProduction(Runnable run);
    }

    private static final HashMap<Class<?>, Logger> classLoggers = new HashMap<>();
    private static final HashMap<String, Logger> namedLoggers = new HashMap<>();

    public static Stopwatch timed(Runnable block) {
        final var watch = Stopwatch.createStarted();
        block.run();
        return watch;
    }

    public static <T> Stopwatch timed(Consumer<T> consumer, T arg) {
        return timed(Runnables.fromConsumer(consumer, arg));
    }

    public static CompletableFuture<Void> completedFuture() {
        return CompletableFuture.completedFuture(null);
    }

    public static CompletableFuture<Void> voidFuture(CompletableFuture<?> future) {
        return CompletableFuture.allOf(future);
    }

    public static CompletableFuture<Void> nullFuture() {
        return CompletableFuture.completedFuture(null);
    }

    public static String loggerFormat(String format, Object... args) {
        toLoggableObjectArray(args);
        return MessageFormatter.arrayFormat(format, args).getMessage();
    }

    public static void toLoggableObjectArray(Object[] objs) {
        for (int i = 0; i < objs.length; i++)
            objs[i] = fromLoggableObject(objs[i]);
    }

    public static Object fromLoggableObject(Object obj) {
        if (obj instanceof Loggable loggable)
            return loggable.toLoggerString();

        return obj;
    }

    public static <T extends Throwable, U> Function<T, U> acceptAndRethrow(Consumer<T> consumer) {
        return throwable -> {
            consumer.accept(throwable);
            uncheckedThrow(throwable);
            return null;
        };
    }

    @NotNull
    public static <T> Supplier<T> unchecked(CheckedSupplier<T> r) {
        return () -> {
            try {
                return r.get();
            } catch (Throwable t) {
                uncheckedThrow(t);

                // unreachable
                return null;
            }
        };
    }

    @NotNull
    public static <T, R> Function<T, R> unchecked(CheckedFunction<T, R> fn) {
        try {
            return t -> {
                try {
                    return fn.apply(t);
                } catch (Throwable e) {
                    uncheckedThrow(e);
                    return null;
                }
            };
        } catch (Throwable t) {
            uncheckedThrow(t);

            // unreachable
            return null;
        }
    }

    public static void uncheckedThrow(Throwable t) {
        doUncheckedThrow(t);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void doUncheckedThrow(Throwable t) throws T {
        throw (T) t;
    }

    public static Logger getLogger(String name) {
        return namedLoggers.computeIfAbsent(name, n -> new LoggableLogger(LoggerFactory.getLogger(n)));
    }

    public static Logger inferLogger() throws Toolbox.Caller.InferenceException {
        return getLogger(Caller.infer().callerClass());
    }

    public static Logger getLogger(Class<?> clazz) {
        return classLoggers.computeIfAbsent(clazz, c -> new LoggableLogger(LoggerFactory.getLogger(c)));
    }

    public static Logger getLogger(String name, String prefix) {
        final var botLogger = getLogger(name);

        if (botLogger instanceof PrefixingLogger logger && logger.getPrefix().equals(prefix))
            return logger;
        else {
            final var logger = new PrefixingLogger(botLogger, prefix);
            namedLoggers.put(name, logger);
            return logger;
        }
    }

    public static Logger getLogger(Class<?> clazz, String prefix) {
        final var botLogger = getLogger(clazz);

        if (botLogger instanceof PrefixingLogger logger && logger.getPrefix().equals(prefix))
            return logger;
        else {
            final var logger = new PrefixingLogger(botLogger, prefix);
            classLoggers.put(clazz, logger);
            return logger;
        }
    }

    public static void testingOnly(Runnable run) {
        if (Ref.currentEnv().isTesting()) {
            run.run();
        }
    }

    public static PerEnvRunner perEnv() {
        return PerEnvRunner.getInstance();
    }
}
