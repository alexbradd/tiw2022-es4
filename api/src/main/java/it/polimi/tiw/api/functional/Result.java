package it.polimi.tiw.api.functional;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Faithful recreation of the Result monad present in languages like Haskell and Rust. It represents the result of a
 * computation that can fail. If the computation succeeds the value returned by the computation is held, otherwise any
 * uncaught exception.
 *
 * @param <E> Type of the exception thrown
 * @param <T> Type of the value held
 */
public class Result<E extends Exception, T> {
    private final Either<E, T> value;

    private Result(Either<E, T> value) {
        this.value = value;
    }

    /**
     * Creates a new Result from a {@link SupplierChecked} that can throw an exception.
     *
     * @param supplier a {@link SupplierChecked} that supplies the value to store in the result
     * @param <E>      The type of the exception thrown in case of failure
     * @param <T>      The type of the value returned in case of success
     * @return a Result representing the status of the computation
     * @throws NullPointerException if {@code supplier} is null
     */
    @SuppressWarnings("unchecked")
    public static <E extends Exception, T> Result<E, T> of(SupplierChecked<T, E> supplier) {
        Objects.requireNonNull(supplier);
        try {
            return ok(supplier.get());
//        Ideally you'd want that, but until then FIXME
//        } catch (RuntimeException e) {
//            throw e;
        } catch (Exception e) {
            return error((E) e);
        }
    }

    /**
     * Creates a new Result from an Exception
     *
     * @param exception the Exception to wrap
     * @param <E>       the type of the Exception
     * @param <T>       the type of the value to hold
     * @return a new Result
     * @throws NullPointerException if {@code exception} is null
     */
    public static <E extends Exception, T> Result<E, T> error(E exception) {
        Objects.requireNonNull(exception);
        return new Result<>(Either.left(exception));
    }

    /**
     * Creates a new Result from a value
     *
     * @param value the Exception to wrap
     * @param <E>   the type of the Exception
     * @param <T>   the type of the value to hold
     * @return a new Result
     * @throws NullPointerException if {@code exception} is null
     */
    public static <E extends Exception, T> Result<E, T> ok(T value) {
        return new Result<>(Either.right(value));
    }

    /**
     * To avoid @SuppressWarning("unchecked")
     */
    private static <E extends Exception, T> Result<E, T> cast(Result<? extends E, ? extends T> r) {
        return r.match(Result::error, Result::ok);
    }

    /**
     * Maps the value stored inside this Result to the correct function.
     *
     * @param error The {@link Function} that will map the value in case an Exception is stored
     * @param ok    The {@link Function} that will map the value in case a value is stored
     * @param <U>   The type of the value returned
     * @return a new value
     * @throws NullPointerException if any parameter is null
     */
    public <U> U match(Function<? super E, ? extends U> error, Function<? super T, ? extends U> ok) {
        Objects.requireNonNull(error);
        Objects.requireNonNull(ok);
        return value.match(error, ok);
    }

    /**
     * Feeds the value stored inside this Result to the correct Consumer.
     *
     * @param error The {@link Consumer} that will accept the value in case an Exception is stored
     * @param ok    The {@link Consumer} that will accept the value in case a value is stored
     * @throws NullPointerException if any parameter is null
     */
    public void consume(Consumer<? super E> error, Consumer<? super T> ok) {
        Objects.requireNonNull(error);
        Objects.requireNonNull(ok);
        value.consume(error, ok);
    }

    /**
     * If holding a value, apply the given mapper and wrap the result in another Result.
     *
     * @param mapper mapper to apply
     * @return a new Result
     * @throws NullPointerException is {@code mapper} is null
     */
    public <U> Result<E, U> map(Function<? super T, ? extends U> mapper) {
        Objects.requireNonNull(mapper);
        return value.match(
                Result::error,
                (T t) -> ok(mapper.apply(t))
        );
    }

    /**
     * If holding a value, apply the given Result bearing mapper. Models Haskell's >>==.
     *
     * @param mapper mapper to apply
     * @return a new ApiResult
     * @throws NullPointerException is {@code mapper} is null
     */
    public <U> Result<E, U> flatMap(Function<? super T, ? extends Result<? extends E, ? extends U>> mapper) {
        Objects.requireNonNull(mapper);
        return value.match(
                Result::error,
                (T t) -> cast(mapper.apply(t))
        );
    }

    /**
     * Discard the value stored in this Result in favour of the one supplied by te given Supplier. If an error is
     * stored, it will be propagated. Models Haskell's >> and *>.
     *
     * @param then the {@link Supplier} that will provide the new Result
     * @param <U>  the type of the new value
     * @return a new Result
     * @throws NullPointerException if {@code then} is null
     */
    public <U> Result<E, U> then(Supplier<Result<? extends E, ? extends U>> then) {
        Objects.requireNonNull(then);
        return flatMap((__) -> then.get());
    }

    /**
     * Execute the given action and discard its Result in favour of the current one. If an error is * stored, it will be
     * propagated. Models Haskell's <*.
     *
     * @param peek the action to execute and discard
     * @param <U>  the type of the discarded value
     * @return a new Result
     * @throws NullPointerException if {@code peek} is null
     */
    public <U> Result<E, T> peek(Function<? super T, Result<? extends E, ? extends U>> peek) {
        Objects.requireNonNull(peek);
        return flatMap(t -> peek.apply(t).then(() -> ok(t)));
    }

    /**
     * Retrieve the value stored in this Result. If an exception is stored, throw it.
     *
     * @return The value stored in the Result
     * @throws E if an exception is stored in the result
     */
    public T get() throws E {
        boolean left = value.match(l -> true, r -> false);
        if (left)
            throw value.fromLeft(null);
        return value.fromRight(null);
    }
}
