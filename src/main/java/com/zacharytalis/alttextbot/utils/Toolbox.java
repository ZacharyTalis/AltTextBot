package com.zacharytalis.alttextbot.utils;

import com.google.common.base.Stopwatch;
import com.zacharytalis.alttextbot.logging.LoggableLogger;
import com.zacharytalis.alttextbot.logging.Logger;
import com.zacharytalis.alttextbot.logging.PrefixingLogger;
import com.zacharytalis.alttextbot.utils.functions.Runnables;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import javax.tools.Tool;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class Toolbox {
    public record Caller(Class<?> callerClass) {
        static class InferenceException extends RuntimeException {
            InferenceException() {
                super("Failed to perform caller inference");
            }
        }

        private static final List<Class<?>> EXCLUDE = List.of(Toolbox.class, Caller.class);

        private static Caller infer() throws InferenceException {
            final var walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
            final var callingClass =
                    walker.walk(
                            s -> s.map(StackWalker.StackFrame::getDeclaringClass)
                                    .filter(Caller::isRelevantClass)
                                    .findFirst()
                    );

            return callingClass.map(Caller::new).orElseThrow(InferenceException::new);
        }

        private static boolean isRelevantClass(Class<?> clazz) {
            return !EXCLUDE.contains(clazz);
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
