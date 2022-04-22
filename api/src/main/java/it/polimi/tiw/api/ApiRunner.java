package it.polimi.tiw.api;

import it.polimi.tiw.api.exceptions.UnavailableDatabaseException;

import java.util.concurrent.Callable;

/**
 * Wraps an {@link ApiResult} bearing function converting eventual unchecked database errors into an {@link ApiResult}.
 */
class ApiRunner {
    /**
     * Executes the given {@link Callable}
     *
     * @param callable the {@link Callable} to execute
     * @param <T>      generic type
     * @return an {@link ApiResult}
     */
    public static <T> ApiResult<T> run(Callable<ApiResult<T>> callable) {
        try {
            return callable.call();
        } catch (UnavailableDatabaseException e) {
            ApiError error = new ApiError(
                    500,
                    "Error while fetching data",
                    new ApiSubError("UnavailableDatabaseException", e.getMessage()));
            return ApiResult.error(error);
        } catch (Exception e) {
            throw new IllegalStateException("API broke", e);
        }
    }
}
