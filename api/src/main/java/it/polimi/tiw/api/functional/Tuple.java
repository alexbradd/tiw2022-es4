package it.polimi.tiw.api.functional;

import java.util.Objects;
import java.util.function.BiFunction;

/**
 * Simple tuple class to hold 2 values.
 *
 * @param <T> the first type to hold
 * @param <U> the second type to hold
 */
public class Tuple<T, U> {
    private final T first;
    private final U second;

    /**
     * Creates a new Tuple with the given values
     *
     * @param first  first value to hold
     * @param second second value to hold
     */
    public Tuple(T first, U second) {
        this.first = first;
        this.second = second;
    }

    /**
     * Getter for the first value stored
     *
     * @return the first value stored
     */
    public T getFirst() {
        return first;
    }

    /**
     * Getter for the second value stored
     *
     * @return the second value stored
     */
    public U getSecond() {
        return second;
    }

    /**
     * Applies the given function to the values stored and returns a new value
     *
     * @param function the {@link BiFunction} to apply
     * @param <V>      the type of the value returned
     * @return a new value
     * @throws NullPointerException if {@code function} is null
     */
    public <V> V match(BiFunction<T, U, V> function) {
        return Objects.requireNonNull(function).apply(first, second);
    }
}
