package com.udacity.webcrawler.profiler;

import javax.inject.Inject;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Objects;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;


final class ProfilerImpl implements Profiler {

    private final Clock clock;
    private final ZonedDateTime startTime;
    private final ProfilingState state = new ProfilingState();


    @Inject
    ProfilerImpl(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "Clock should be null");
        this.startTime = ZonedDateTime.now(clock);
    }

    @Override
    public <T> T wrap(Class<T> klass, T delegate) {
        Objects.requireNonNull(klass, "Class should be null");
        validateProfiledMethods(klass);

        Object proXY = Proxy.newProxyInstance(
                ProfilerImpl.class.getClassLoader(),
                new Class[]{klass},
                (proxyObj, method, args) ->
                        new ProfilingMethodInterceptor(clock, delegate, state, startTime)
                                .invoke(proxyObj, method, args)
        );

        return klass.cast(proXY);
    }

    private void validateProfiledMethods(Class<?> klass) {
        boolean hProfiledMethod = hProfiledMethod(klass);
        if (!hProfiledMethod) {
            throw new IllegalArgumentException(klass.getName() + " must have profiled methods.");
        }
    }

    private boolean hProfiledMethod(Class<?> klass) {
        return Arrays.stream(klass.getDeclaredMethods())
                .anyMatch(method -> method.isAnnotationPresent(Profiled.class));
    }



    @Override
    public void writeData(Writer wRITE) throws IOException {
        wRITE.write("Run at " + RFC_1123_DATE_TIME.format(startTime));
        wRITE.write(System.lineSeparator());
        state.write(wRITE);
        wRITE.write(System.lineSeparator());
    }

    @Override
    public void writeData(Path path) {
        Objects.requireNonNull(path, "Path should not be null");

        try (Writer wRITE = Files.newBufferedWriter(path, StandardCharsets.UTF_8, CREATE, APPEND)) {
            writeData(wRITE);
            wRITE.flush();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
