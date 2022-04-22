package it.polimi.tiw.api.functional;

import java.util.function.Supplier;

/**
 * Like {@link Supplier}, but throws a checked Exception.
 *
 * @param <T> The type of the value supplied
 * @param <E> The type of exception thrown
 * @see Supplier
 */
@FunctionalInterface
public interface SupplierChecked<T, E extends Exception> {
    T get() throws E;
}
