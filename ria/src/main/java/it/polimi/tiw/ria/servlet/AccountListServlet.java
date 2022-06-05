package it.polimi.tiw.ria.servlet;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import it.polimi.tiw.api.AccountFacade;
import it.polimi.tiw.api.beans.Account;
import it.polimi.tiw.api.dbaccess.ProductionConnectionRetriever;
import it.polimi.tiw.api.error.Errors;
import it.polimi.tiw.api.functional.ApiResult;
import it.polimi.tiw.api.functional.Tuple;
import it.polimi.tiw.ria.auth.AuthUtils;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static it.polimi.tiw.ria.servlet.ServletUtils.*;

/**
 * Endpoint for retrieving the accounts of a user. It accepts POST requests containing a JSON object formatted as such:
 *
 * <ul>
 *     <li>{@code token}: a valid authentication token</li>
 *     <li>{@code userId}: the id of the user of which the accounts will be retrieved</li>
 *     <li>{@code detailed}: a boolean value specifying if personal information should be included or not</li>
 * </ul>
 * <p>
 * The request must provide a valid authentication token, or the request will be rejected with a 401. Every user has
 * access to the non-detailed view of the account list ({@code detailed = false}), while only the owner of said accounts
 * can retrieve the full view. If this rule is not respected, the request will be rejected with a 403 error.
 * <p>
 * The endpoint will respond with a JSON object containing an array of objects in its {@code accounts} field. Each
 * account object will be formatted as follows:
 * <p>
 * <pre>
 *     {
 *       "base64Id": ...,
 *       "ownerId": ...,
 *       "balance": ... // only in detailed view
 *     }
 * </pre>
 * <p>
 * If any error has been encountered, a suitable JSON object describing the error is attached in the response.
 */
@WebServlet("/api/accounts/ofUser")
public class AccountListServlet extends HttpServlet {
    private String iss, tokenSecret;

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

        Gson gson = new Gson();
        Request request = parseRequest(gson, req);
        if (request == null) {
            sendInvalidFormatError(resp);
            return;
        }

        Tuple<Integer, JsonObject> res = ProductionConnectionRetriever.getInstance()
                .with(c -> AccountFacade.withDefaultObjects(c).ofUser(request.userId))
                .peek(accounts -> checkPermissions(request.token, request.userId, request.detailed))
                .map(accounts -> toJson(gson, accounts, request.detailed))
                .match(accountObjs -> {
                            JsonObject obj = new JsonObject();
                            obj.addProperty("type", "OK");
                            obj.add("accounts",
                                    accountObjs.collect(JsonArray::new, JsonArray::add, JsonArray::addAll));
                            return new Tuple<>(200, obj);
                        },
                        e -> new Tuple<>(e.statusCode(), fromApiErrorToJSON(e)));
        sendJson(resp, res.getFirst(), res.getSecond());
    }

    private ApiResult<?> checkPermissions(String token, String userId, boolean detailed) {
        try {
            DecodedJWT jwt = AuthUtils.verifyToken(token, iss, tokenSecret);
            String jwtUserId = jwt.getClaim("userId").asString();
            if (Objects.equals(jwtUserId, userId))
                return ApiResult.ok(true);
            else
                return detailed
                        ? ApiResult.error(Errors.fromPermissionDenied("accounts"))
                        : ApiResult.ok(false);
        } catch (JWTVerificationException | NullPointerException e) {
            return ApiResult.error(Errors.fromUnauthorized());
        }
    }

    private Request parseRequest(Gson gson, HttpServletRequest req) throws IOException {
        Request ret;
        try (JsonReader reader = new JsonReader(req.getReader())) {
            ret = gson.fromJson(reader, Request.class);
            return ret == null || ret.userId == null ? null : ret;
        } catch (JsonParseException | IllegalStateException | ClassCastException e) {
            return null;
        }
    }

    private Stream<JsonObject> toJson(Gson gson, List<Account> accounts, boolean conserve) {
        return accounts.stream()
                .map(gson::toJsonTree)
                .map(JsonElement::getAsJsonObject)
                .peek(j -> {
                    if (!conserve)
                        j.remove("balance");
                });
    }

    private static class Request {
        private String userId;
        private String token;
        private boolean detailed;
    }
}
