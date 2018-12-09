package util.common;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.time.Duration;
import java.util.Optional;

/**
 * Simple requests library for making and sending http requests.
 */
public class Requests {
    private static final TimeBasedCache<String, JsonObject> cache =
            TimeBasedCache.withMaxDuration(Duration.ofMinutes(1));

    /**
     * Sends a get request to a given url, returning the response body parsed into json. Any exceptions are swallowed
     * into an empty optional b/c I'm lazy. Original exceptions should be handled in calling code.
     * <p>
     * Each successful request is cached for 1 minute.
     */
    public static Optional<JsonObject> getJson(String url) {
        return cache.computeIfAbsent(url, endpoint -> {
            InputStream input = new URL(endpoint).openStream();
            return new JsonParser()
                    .parse(new InputStreamReader(input))
                    .getAsJsonObject();
        });

    }
}
