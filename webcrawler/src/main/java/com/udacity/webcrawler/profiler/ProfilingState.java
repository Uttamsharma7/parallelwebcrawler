package com.udacity.webcrawler.profiler;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;


public final class ProfilingState {
    private final Map<String, Map<Long, AtomicLong>> caCountsByThread = new ConcurrentHashMap<>();
    private final Map<String, Duration> tDurations = new ConcurrentHashMap<>();


    public void record(Class<?> callingClass, Method method, Duration elapsed, long threadId) {
        Objects.requireNonNull(callingClass);
        Objects.requireNonNull(method);
        Objects.requireNonNull(elapsed);

        if (elapsed.isNegative()) {
            throw new IllegalArgumentException("negative elapsed time");
        }
        String key = formatMethodCall(callingClass, method);

        caCountsByThread
                .computeIfAbsent(key, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(threadId, t -> new AtomicLong())
                .addAndGet(1);

        tDurations.compute(key, (k, v) -> (v == null) ? elapsed : v.plus(elapsed));
    }

    public static String formatMethodCall(Class<?> callingClass, Method method) {
        return String.format("%s#%s", callingClass.getName(), method.getName());
    }

    public void write(Writer writer) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();

        caCountsByThread.forEach((methodCall, threadCounts) -> {
            //calculating total innvocations for method across all the threads:-
            long tInvocations = threadCounts.values().stream()
                    .mapToLong(AtomicLong::get)
                    .sum();

            // Calculating total duration for the method across all the threads:-
            Duration totalDuration = tDurations.get(methodCall);

            // Append method information
            stringBuilder.append(methodCall)
                    .append(" took ")
                    .append(formatDuration(totalDuration))
                    .append(" (called ")
                    .append(tInvocations)
                    .append(" times)")
                    .append(System.lineSeparator());


            //appended invocations and average duration/thread:-
            threadCounts.forEach((threadId, invocations) -> {
                // Calculate average duration per thread
                Duration threadTDuration = tDurations.get(methodCall);
                Duration threadAverageDuration = threadTDuration.dividedBy(invocations.get());

                stringBuilder.append("[Thread ID: ")
                        .append(threadId)
                        .append(" (called ")
                        .append(invocations.get())
                        .append(" times)] - Average duration:- ")
                        .append(formatDuration(threadAverageDuration))
                        .append(System.lineSeparator());
            });

            stringBuilder.append(System.lineSeparator());
        });

        writer.write(stringBuilder.toString());
    }

    /**
     * Formats the duration as a string in the format "m minutes s seconds ms milliseconds".
     *
     * @param duration The duration to format.
     * @return The formatted duration string.
     */
    public static String formatDuration(Duration duration) {
        return String.format(
                "%sm %ss %sms", duration.toMinutes(), duration.toSecondsPart(), duration.toMillisPart());
    }
}
