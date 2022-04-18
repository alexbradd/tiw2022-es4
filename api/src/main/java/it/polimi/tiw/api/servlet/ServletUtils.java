package it.polimi.tiw.api.servlet;

import it.polimi.tiw.api.exceptions.UpdateException;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Objects;

/**
 * Static class containing some utilities used by various servlets.
 */
class ServletUtils {

    private ServletUtils() {
    }

    public static void executeLogicAndHandleErrors(HttpServletRequest req, HttpServletResponse resp, ServletLogic r) throws IOException {
        try {
            r.run();
        } catch (NullPointerException e) {
            setStatusAndWriteError(resp,
                    400,
                    constructError("Missing required parameter", e.getMessage()));
        } catch (IllegalArgumentException e) {
            setStatusAndWriteError(resp,
                    400,
                    constructError("Malformed parameter", e.getMessage()));
        } catch (UpdateException e) {
            setStatusAndWriteError(resp,
                    400,
                    constructError("Update error", e.toString()));
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            setStatusAndWriteError(resp,
                    500,
                    constructError("An internal error occurred", sw.toString()));
        }
    }

    /**
     * Construct an error JSON object. It will contain two entries: {@code msg}, a natural language message, and
     * {@code extra}, a string containing additional information.
     *
     * @param message    the natural language message
     * @param additional the additional information
     * @return a {@link JsonObject}
     * @throws NullPointerException if {@code message} is null
     */
    public static JsonObject constructError(String message, String additional) {
        Objects.requireNonNull(message);
        JsonObjectBuilder builder = Json.createObjectBuilder().add("msg", message);
        if (additional != null)
            builder = builder.add("extra", additional);
        return builder.build();
    }

    /**
     * Construct an error JSON object. It will contain two entries: {@code msg}, a natural language message, and
     * {@code extra}, a string containing additional information.
     *
     * @param message the natural language message
     * @return a {@link JsonObject}
     * @throws NullPointerException if {@code message} is null
     */
    public static JsonObject constructError(String message) {
        return constructError(message, null);
    }

    /**
     * Sets the status code of the {@link HttpServletResponse} and writes the given {@link JsonObject} into the
     * response.
     *
     * @param r     the {@link HttpServletResponse}
     * @param error the HTTP status code
     * @param err   the {@link JsonObject} to write
     * @throws IOException if an IO error occurs
     */
    public static void setStatusAndWriteError(HttpServletResponse r, int error, JsonObject err) throws IOException {
        r.setStatus(error);
        r.getWriter().print(err);
    }
}
