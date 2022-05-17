package it.polimi.tiw.api.error;

import com.google.gson.JsonObject;

import java.util.Objects;

/**
 * Simple data holder for more detailed information about an error. It is contained in a {@link ApiError}.
 */
public record ApiSubError(String reason, String message) {
    /**
     * Creates a new ApiSubError with the specified parameters
     *
     * @param reason  the reason for the error
     * @param message an additional message
     * @throws NullPointerException if any parameter is null
     */
    public ApiSubError(String reason, String message) {
        this.reason = Objects.requireNonNull(reason);
        this.message = Objects.requireNonNull(message);
    }

    /**
     * Returns a json representation of this object.
     *
     * @return a JsonObject corresponding to this object
     */
    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("reason", reason);
        obj.addProperty("message", message);
        return obj;
    }
}
