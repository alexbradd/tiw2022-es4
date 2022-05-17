package it.polimi.tiw.api.functional;

import it.polimi.tiw.api.error.ApiError;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Similar to {@link Result}, however it is less general and more domain specific. It represents the status of a
 * computation. In case of success, an object of the specified type will be held; if a failure of any kind is held, a
 * {@link ApiError} will be stored inside detailing exactly what's happened.
 *
 * @param <T> the type of the object enclosed in it
 */
public class ApiResult<T> {
    private final Either<ApiError, T> value;

    private ApiResult(Either<ApiError, T> value) {
        this.value = value;
    }

    /**
     * Create a new ApiResult holding a success and the given value.
     *
     * @param value the value to hold
     * @param <U>   the type of the value
     * @return a new ApiResult
     */
    public static <U> ApiResult<U> ok(U value) {
        return new ApiResult<>(Either.right(value));
    }

    /**
     * Create a new ApiResult holding a failure and the given {@link ApiError}.
     *
     * @param error the error to hold
     * @return a new ApiResult
     * @throws NullPointerException is {@code error} is null
     */
    public static <U> ApiResult<U> error(ApiError error) {
        Objects.requireNonNull(error);
        return new ApiResult<>(Either.left(error));
    }

    /**
     * To avoid @SuppressWarning("unchecked")
     */
    private static <T> ApiResult<T> cast(ApiResult<? extends T> result) {
        return result.match(ApiResult::ok, ApiResult::error);
    }

    /**
     * If holding a success, apply the given mapper and wrap the result in another ApiResult.
     *
     * @param mapper mapper to apply
     * @return a new ApiResult
     * @throws NullPointerException is {@code mapper} is null
     */
    public <U> ApiResult<U> map(Function<? super T, ? extends U> mapper) {
        Objects.requireNonNull(mapper);
        return value.match(
                ApiResult::error,
                (T t) -> ok(mapper.apply(t))
        );
    }

    /**
     * If holding a success, apply the given ApiBearing bearing mapper.
     *
     * @param mapper mapper to apply
     * @return a new ApiResult
     * @throws NullPointerException is {@code mapper} is null
     */
    public <U> ApiResult<U> flatMap(Function<? super T, ? extends ApiResult<? extends U>> mapper) {
        Objects.requireNonNull(mapper);
        return value.match(ApiResult::error, (T t) -> cast(mapper.apply(t)));
    }

    /**
     * Discard the value stored in this ApiResult in favour of the one supplied by te given Supplier. If an error is
     * stored, it will be propagated. Models Haskell's >> and *>.
     *
     * @param then the {@link Supplier} that will provide the new ApiResult
     * @param <U>  the type of the new value
     * @return a new ApiResult
     * @throws NullPointerException if {@code then} is null
     */
    public <U> ApiResult<U> then(Supplier<ApiResult<? extends U>> then) {
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
    public <U> ApiResult<T> peek(Function<? super T, ApiResult<? extends U>> peek) {
        Objects.requireNonNull(peek);
        return flatMap(t -> peek.apply(t).then(() -> ok(t)));
    }

    /**
     * If an element is present returns it, otherwise throw NoSuchElementException
     *
     * @return the element stored inside
     * @throws NoSuchElementException if the ApiResult contains an error
     */
    public T get() {
        return match(
                t -> t,
                e -> {
                    throw new NoSuchElementException("An error was stored");
                });
    }

    /**
     * Opposite of {@link #get()}.
     *
     * @return the error stored inside
     * @throws NoSuchElementException if the ApiResult contains a value.
     */
    public ApiError getError() {
        return match(
                t -> {
                    throw new NoSuchElementException("A value was stored");
                },
                e -> e);
    }

    /**
     * Maps the state contained in ths object with a value.
     *
     * @param success {@link Function} that will map the wrapped object in case of success
     * @param failure {@link Function} that will map the wrapped {@link ApiError} in case of error
     * @param <U>     the type of the value to return
     * @return an object of type {@link U}
     * @throws NullPointerException if any parameter is null
     */
    public <U> U match(Function<? super T, ? extends U> success,
                       Function<? super ApiError, ? extends U> failure) {
        Objects.requireNonNull(success);
        Objects.requireNonNull(failure);
        return value.match(failure, success);
    }

    /**
     * Consumes the objects stored in this ApiResult
     *
     * @param success {@link Consumer} for the success object
     * @param failure {@link Consumer} for the {@link ApiError}
     * @throws NullPointerException if any parameter is null
     */
    public void consume(Consumer<? super T> success, Consumer<? super ApiError> failure) {
        Objects.requireNonNull(success);
        Objects.requireNonNull(failure);
        value.consume(failure, success);
    }
}
