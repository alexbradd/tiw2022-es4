package it.polimi.tiw.ria.servlet;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.JsonObject;
import it.polimi.tiw.api.error.ApiError;
import it.polimi.tiw.api.error.Errors;
import it.polimi.tiw.api.functional.Tuple;
import it.polimi.tiw.ria.auth.AuthUtils;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

import static it.polimi.tiw.ria.servlet.ServletUtils.fromApiErrorToJSON;
import static it.polimi.tiw.ria.servlet.ServletUtils.sendJson;

/**
 * Issues a new access token for the user with id contained in the refresh token contained in the {@code refresh}
 * cookie.
 * <p>
 * It accepts GET requests. If the request does not bear a {@code refresh} cookie or if it is not valid (e.g. it has
 * expired), the servlet will respond with a 401 response and a suitable JSON object. If everything is ok, a 200 is sent
 * containing a JSON object with the new token.
 */
@WebServlet("/api/auth/refresh")
public class RefreshServlet extends HttpServlet {
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
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Cookie[] cookies = req.getCookies();
        if (cookies == null) {
            ApiError e = Errors.fromNullParameter("refresh_cookie");
            sendJson(resp, e.statusCode(), e.toJson());
            return;
        }

        Optional<Cookie> refresh = Arrays.stream(cookies)
                .filter(c -> c.getName().equals("refresh"))
                .findAny();

        Tuple<Integer, JsonObject> res = refresh.flatMap(c -> {
            String token = c.getValue();
            try {
                DecodedJWT jwt = AuthUtils.verifyRefreshToken(token, iss, refreshSecret);
                String userId = jwt.getClaim("userId").asString();
                if (userId == null)
                    return Optional.empty();
                return Optional.of(AuthUtils.newToken(userId, iss, tokenSecret));
            } catch (JWTVerificationException e) {
                System.out.println("verification failed");
                e.printStackTrace();
                return Optional.empty();
            }
        }).map(token -> {
            JsonObject obj = new JsonObject();
            obj.addProperty("type", "OK");
            obj.addProperty("token", token);
            return new Tuple<>(200, obj);
        }).orElseGet(() -> {
            ApiError e = Errors.fromUnauthorized();
            return new Tuple<>(e.statusCode(), fromApiErrorToJSON(e));
        });
        sendJson(resp, res.getFirst(), res.getSecond());
    }
}
