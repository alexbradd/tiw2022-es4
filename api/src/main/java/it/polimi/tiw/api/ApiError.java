package it.polimi.tiw.api;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
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
        JsonObjectBuilder builder = Json.createObjectBuilder()
                .add("error", Json.createObjectBuilder()
                        .add("code", statusCode)
                        .add("message", errorMessage));
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        for (ApiSubError e : errors)
            arrayBuilder.add(e.toJson());
        builder.add("errors", arrayBuilder);
        return builder.build();
    }
}

