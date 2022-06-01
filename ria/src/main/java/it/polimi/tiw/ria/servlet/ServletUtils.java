package it.polimi.tiw.ria.servlet;

import com.google.gson.JsonElement;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Miscellaneous utilities
 */
public class ServletUtils {
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
}
