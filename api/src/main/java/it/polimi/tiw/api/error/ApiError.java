package it.polimi.tiw.api.error;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import it.polimi.tiw.api.functional.ApiResult;

import java.util.Objects;

/**
 * Simple data holder for information about an error. It is contextualized by a {@link ApiResult}.
 */
public record ApiError(int statusCode, String errorMessage, ApiSubError... errors) {
    /**
     * Creates a new ApiError with the specified parameters
     *
     * @param statusCode   the status code
     * @param errorMessage the error message
     * @param errors       an array of {@link ApiSubError}
     * @throws NullPointerException if any parameter is null
     */
    public ApiError(int statusCode, String errorMessage, ApiSubError... errors) {
        this.statusCode = statusCode;
        this.errorMessage = Objects.requireNonNull(errorMessage);
        this.errors = Objects.requireNonNull(errors);
    }

    /**
     * Returns a json representation of this object.
     *
     * @return a JsonObject corresponding to this object
     */
    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        JsonObject error = new JsonObject();
        JsonArray errorArr = new JsonArray();

        error.addProperty("code", statusCode);
        error.addProperty("message", errorMessage);
        for (ApiSubError e : errors)
            errorArr.add(e.toJson());

        obj.add("error", error);
        obj.add("errors", errorArr);
        return obj;
    }
}

