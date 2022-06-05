package it.polimi.tiw.ria.servlet;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import it.polimi.tiw.api.AccountFacade;
import it.polimi.tiw.api.beans.Account;
import it.polimi.tiw.api.beans.User;
import it.polimi.tiw.api.dbaccess.ProductionConnectionRetriever;
import it.polimi.tiw.api.error.ApiError;
import it.polimi.tiw.api.error.Errors;
import it.polimi.tiw.api.functional.Result;
import it.polimi.tiw.api.functional.Tuple;
import it.polimi.tiw.ria.auth.AuthUtils;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static it.polimi.tiw.ria.servlet.ServletUtils.*;

/**
 * Endpoint for creating a new {@link Account} for the currently logged in {@link User}. It requires a valid JSON
 * object with a {@code token} filed containing a valid authentication ticket. If such requirements are not met, the
 * request will be rejected with a 401.
 * <p>
 * If an error occurred, a suitable error JSON object is returned with the response.
 */
@WebServlet("/api/accounts")
public class NewAccountServlet extends HttpServlet {
    public String iss, tokenSecret;

    /**
     * {@inheritDoc}
     */
    @Override
    public void init() {
        iss = getServletContext().getInitParameter("ISSUER");
        tokenSecret = getServletContext().getInitParameter("TOKEN_SECRET");
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

        String token = getToken(req);
        if (token == null) {
            sendInvalidFormatError(resp);
            return;
        }

        Tuple<Integer, JsonObject> res = Result.of(() -> {
                    DecodedJWT jwt = AuthUtils.verifyToken(token, iss, tokenSecret);
                    return jwt.getClaim("userId").asString();
                })
                .map(s -> ProductionConnectionRetriever.getInstance()
                        .with(c -> AccountFacade.withDefaultObjects(c).createFor(s)))
                .match(ex -> {
                            ApiError err = Errors.fromUnauthorized();
                            return new Tuple<>(err.statusCode(), fromApiErrorToJSON(err));
                        },
                        result -> result.match(
                                a -> {
                                    JsonObject obj = new JsonObject();
                                    obj.addProperty("type", "OK");
                                    return new Tuple<>(200, obj);
                                },
                                e -> new Tuple<>(e.statusCode(), fromApiErrorToJSON(e))));
        sendJson(resp, res.getFirst(), res.getSecond());
    }

    private String getToken(HttpServletRequest req) throws IOException {
        try (JsonReader reader = new JsonReader(req.getReader())) {
            JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
            return obj.getAsJsonPrimitive("token").getAsString();
        } catch (JsonParseException | IllegalStateException | NullPointerException | ClassCastException e) {
            return null;
        }
    }
}
