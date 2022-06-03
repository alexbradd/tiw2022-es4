package it.polimi.tiw.ria.servlet;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.polimi.tiw.api.error.ApiError;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;

/**
 * Miscellaneous utilities
 */
public class ServletUtils {
    /**
     * Returns true if the given request has not a JSON Content-Type header.
     *
     * @param req the {@link HttpServletRequest}
     * @return true if the given request has not a JSON Content-Type header
     */
    public static boolean hasNotJSONContentType(HttpServletRequest req) {
        return !Objects.equals(req.getContentType(), "application/json");
    }

    /**
     * Sends a formatted JSON error message with status code 400. To be used when the request does not contain JSON.
     *
     * @param res the {@link HttpServletResponse}
     * @throws IOException if an IO error is encountered
     */
    public static void sendWrongTypeError(HttpServletResponse res) throws IOException {
        sendJson(res, 400, fromApiErrorToJSON(new ApiError(400, "Wrong content type")));
    }

    /**
     * Sends a formatted JSON error message with status code 400. To be used when the request does contain JSON, but
     * is incompatible with the format expected (e.g. excepting an object and getting a string).
     *
     * @param res the {@link HttpServletResponse}
     * @throws IOException if an IO error is encountered
     */
    public static void sendInvalidFormatError(HttpServletResponse res) throws IOException {
        sendJson(res, 400, fromApiErrorToJSON(new ApiError(400, "Object is not formatted correctly")));
    }

    /**
     * Sets the status to the given integer and writes the given JSON in the response.
     *
     * @param res    the {@link HttpServletResponse}
     * @param status the status
     * @param json   the JSON
     * @throws IOException if an IO error is encountered
     */
    public static void sendJson(HttpServletResponse res, int status, JsonElement json) throws IOException {
        res.setStatus(status);
        res.setContentType("application/json");
        res.getWriter().println(json.toString());
    }

    /**
     * Converts an {@link ApiError} to an error JSON object.
     *
     * @param err the {@link ApiError}
     * @return a {@link JsonObject}
     */
    public static JsonObject fromApiErrorToJSON(ApiError err) {
        JsonObject obj = new JsonObject();
        obj.addProperty("type", "ERROR");
        obj.add("error", err.toJson());
        return obj;
    }
}
