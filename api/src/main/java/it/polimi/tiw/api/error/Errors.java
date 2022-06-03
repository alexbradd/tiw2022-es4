package it.polimi.tiw.api.error;

import java.sql.SQLException;

/**
 * Utility class for constructing standard {@link ApiError}
 */
public class Errors {
    private Errors() {
    }

    /**
     * Create a new {@link ApiError} wrapping a {@link SQLException}. The status code is 500.
     *
     * @param exception the {@link SQLException} to wrap
     * @return a new {@link ApiError}
     */
    public static ApiError fromSQLException(SQLException exception) {
        return new ApiError(500,
                "Error while fetching data",
                new ApiSubError("SQLException", exception.getMessage() == null ? "" : exception.getMessage()));
    }

    /**
     * Creates a new {@link ApiError} signaling a null parameter. Status code is 400.
     *
     * @param param the name of the parameter that was null.
     * @return a new {@link ApiError}
     */
    public static ApiError fromNullParameter(String param) {
        return new ApiError(400,
                "Required parameter is missing",
                new ApiSubError("NullPointerException", "Required parameter " + param + " is null"));
    }

    /**
     * Creates a new {@link ApiError} signaling a malformed parameter. Status code is 400.
     *
     * @param param the name of the parameter that has invalid format
     * @return a new {@link ApiError}
     */
    public static ApiError fromMalformedParameter(String param) {
        return new ApiError(400,
                "Parameter is malformed",
                new ApiSubError("IllegalArgumentException", "Parameter " + param + " is of invalid format"));
    }

    /**
     * Creates a new {@link ApiError} for when an object with the given specifier cannot be found. Status code is 404.
     *
     * @param specifier the field by which the search was conducted
     * @return a new {@link ApiError}
     */
    public static ApiError fromNotFound(String specifier) {
        return new ApiError(404,
                "Cannot find this object",
                new ApiSubError("NoSuchElementException", "No such object with given " + specifier));
    }

    /**
     * Creates a new {@link ApiError} for when the value of a parameter conflicts with data stored on the server. Status
     * code is 409.
     *
     * @param param the name of the parameter that conflicts
     * @return a new {@link ApiError}
     */
    public static ApiError fromConflict(String param) {
        return new ApiError(409,
                "Parameter conflicts with the current state",
                new ApiSubError("IllegalArgumentException", "Parameter" + param + " conflicts with the data on the server"));
    }

    /**
     * Creates a new {@link ApiError} for when a resource is not accessible to the requester. Status code is 403.
     *
     * @param res the name of the resource that was requested
     * @return a new {@link ApiError}
     */
    public static ApiError fromPermissionDenied(String res) {
        return new ApiError(403,
                "User cannot access this resource",
                new ApiSubError("IllegalAccessException", "Cannot view resource " + res));
    }

    /**
     * Creates a new {@link ApiError} for when a resource is not accessible because the user is not authenticated.
     * Status code is 401.
     *
     * @return a new {@link ApiError}
     */
    public static ApiError fromUnauthorized() {
        return new ApiError(401, "You are not authenticated");
    }
}
