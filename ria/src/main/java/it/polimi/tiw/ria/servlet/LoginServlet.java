package it.polimi.tiw.ria.servlet;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonReader;
import it.polimi.tiw.api.UserFacade;
import it.polimi.tiw.api.beans.LoginRequest;
import it.polimi.tiw.api.dbaccess.ProductionConnectionRetriever;
import it.polimi.tiw.api.functional.Tuple;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static it.polimi.tiw.ria.auth.AuthUtils.newRefreshToken;
import static it.polimi.tiw.ria.auth.AuthUtils.newToken;
import static it.polimi.tiw.ria.servlet.ServletUtils.*;

/**
 * Endpoint for user login. It accepts POST requests containing a JSON object with the following fields:
 *
 * <ol>
 *     <li>'username': the User's username</li>
 *     <li>'clearPassword': the User's clear text password</li>
 * </ol>
 * <p>
 * In case of success, the servlet will create a new access token and return it in the response. Otherwise, it will
 * return a JSON object containing information about what happened
 */
@WebServlet("/api/auth/login")
public class LoginServlet extends HttpServlet {
    private String iss, tokenSecret, refreshSecret;

    /**
     * {@inheritDoc}
     */
    @Override
    public void init() {
        iss = getServletContext().getInitParameter("ISSUER");
        tokenSecret = getServletContext().getInitParameter("TOKEN_SECRET");
        refreshSecret = getServletContext().getInitParameter("REFRESH_SECRET");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (hasNotJSONContentType(req)) {
            sendWrongTypeError(resp);
            return;
        }

        try (JsonReader jsonReader = new JsonReader(req.getReader())) {
            LoginRequest loginRequest = new Gson().fromJson(jsonReader, LoginRequest.class);
            Tuple<Integer, JsonObject> res = ProductionConnectionRetriever.getInstance()
                    .with(c -> UserFacade.withDefaultObjects(c).authorize(loginRequest))
                    .match(u -> {
                        String accessToken = newToken(u.getBase64Id(), iss, tokenSecret);
                        String refreshToken = newRefreshToken(u.getBase64Id(), iss, refreshSecret);

                        JsonObject obj = new JsonObject();
                        obj.addProperty("type", "OK");
                        obj.addProperty("token", accessToken);

                        Cookie refreshCookie = new Cookie("refresh", refreshToken);
                        refreshCookie.setHttpOnly(true);
                        resp.addCookie(refreshCookie);

                        return new Tuple<>(200, obj);
                    }, e -> new Tuple<>(e.statusCode(), fromApiErrorToJSON(e)));
            sendJson(resp, res.getFirst(), res.getSecond());
        } catch (JsonParseException e) {
            sendInvalidFormatError(resp);
        }
    }
}
