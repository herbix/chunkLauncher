package io.chaofan.chunklauncher.util;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class StreamUtil {
    public static <T> Stream<T> toStream(Iterable<T> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false);
    }
}
