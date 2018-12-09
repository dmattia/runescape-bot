package util.common;

import java.util.Optional;

/**
 * Similar to Function<T, R> but can throw exceptions and has fewer methods for composition.
 */
@FunctionalInterface
public interface ExceptionThrowingFunction<T, R> {
    /**
     * Applies this function to the given argument.
     *
     * @param t the function argument
     * @return the function result
     */
    R apply(T t) throws Exception;

    /**
     * Applies this function to the given argument, swallowing any exceptions and returning the empty optional
     * if they occur. In production, the exceptions would be important, but for these scripts I don't really
     * care if they occur, and can accept an empty response on any failure.
     */
    default Optional<R> applyIgnoringExceptions(T t) {
        try {
            return Optional.of(apply(t));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
