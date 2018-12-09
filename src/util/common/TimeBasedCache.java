package util.common;

import org.rspeer.runetek.api.commons.Pair;
import org.rspeer.runetek.api.commons.StopWatch;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A cache where each key should only map to a value for a maximum amount of time. This is useful for cases where
 * a certain level of data freshness is expected.
 */
public class TimeBasedCache<K, V> {
    private final Map<K, Pair<StopWatch, Optional<V>>> cache;
    private final Duration maxDuration;

    /**
     * Only create TimeBasedCache instances from the factory method.
     */
    private TimeBasedCache(Duration maxDuration) {
        this.cache = new HashMap<>();
        this.maxDuration = maxDuration;
    }

    /**
     * Creates a cache with a fixed maximum duration that all new cache elements will use as the freshness requirement.
     */
    public static TimeBasedCache withMaxDuration(Duration maxDuration) {
        return new TimeBasedCache(maxDuration);
    }

    /**
     * Computes a value for a given key, using the cached value if it is present and within the maximum duration
     * a value is fresh for.
     */
    public Optional<V> computeIfAbsent(K key, ExceptionThrowingFunction<K, V> computeFunction) {
        if (!cache.containsKey(key) || cache.get(key).getLeft().exceeds(maxDuration)) {
            cache.put(key, new Pair<>(StopWatch.start(), computeFunction.applyIgnoringExceptions(key)));
        }
        return cache.get(key).getRight();
    }
}
