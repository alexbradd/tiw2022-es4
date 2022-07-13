package it.polimi.tiw.ria.servlet;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import it.polimi.tiw.api.AccountFacade;
import it.polimi.tiw.api.TransferFacade;
import it.polimi.tiw.api.beans.Account;
import it.polimi.tiw.api.beans.InstantTypeAdapter;
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
import java.time.Instant;
import java.util.Objects;

import static it.polimi.tiw.ria.servlet.ServletUtils.*;

/**
 * Endpoints for receiving the transfer list of a given account. It accepts POST requests containing a JSON object
 * formatted as such:
 *
 * <ul>
 *     <li>{@code accountId}: the id of the account to query</li>
 *     <li>{@code token}: the identification token</li>
 * </ul>
 * <p>
 * The request must provide a valid authorization code or the request will be rejected with a 401. Moreover, the user
 * identified by the given token needs to be the owner of the account queried or the access to the resource will be
 * prohibited and request rejected with a 403.
 * <p>
 * The endpoint will respond with a JSON object containing two array of objects representing the incoming and outgoing
 * transfers. Each transfer object will be formatted as such:
 *
 * <pre>
 *     {
 *         "base64Id": ...,
 *         "date": ..., // Date string parseable by Date.parse()
 *         "amount": ...,
 *         "toId": ...,
 *         "toBalance": ...,
 *         "fromId": ...,
 *         "fromBalance": ...,
 *     }
 * </pre>
 * <p>
 * If any error has been encountered, an error object will be attached to the response.
 */
@WebServlet("/api/accounts/transfers")
public class AccountDetailsServlet extends HttpServlet {
    private String iss, tokenSecret;
    private Gson gson;

    /**
     * {@inheritDoc}
     */
    @Override
    public void init() {
        iss = getServletContext().getInitParameter("ISSUER");
        tokenSecret = getServletContext().getInitParameter("TOKEN_SECRET");
        gson = new GsonBuilder()
                .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
                .create();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Tuple<Integer, JsonObject> res = checkRequestFormat(gson, req, Request.class, (r) -> r.accountId == null)
                .flatMap((request) -> ProductionConnectionRetriever.getInstance()
                        .with(c -> {
                            AccountFacade accounts = AccountFacade.withDefaultObjects(c);
                            TransferFacade transfers = TransferFacade.withDefaultObjects(c);
                            return validateToken(request.token)
                                    .flatMap(userId -> accounts
                                            .byId(request.accountId)
                                            .flatMap(a -> checkPermissions(a, userId)))
                                    .flatMap(a -> transfers.of(a.getBase64Id()));
                        }))
                .match(
                        data -> {
                            JsonObject obj = new JsonObject();
                            JsonArray incoming = listToJsonArray(gson, data.getFirst());
                            JsonArray outgoing = listToJsonArray(gson, data.getSecond());
                            obj.addProperty("type", "OK");
                            obj.add("incoming", incoming);
                            obj.add("outgoing", outgoing);
                            return new Tuple<>(200, obj);
                        },
                        err -> new Tuple<>(err.statusCode(), fromApiErrorToJSON(err))
                );
        sendJson(resp, res.getFirst(), res.getSecond());
    }

    private ApiResult<String> validateToken(String token) {
        try {
            DecodedJWT jwt = AuthUtils.verifyToken(token, iss, tokenSecret);
            String jwtUserId = jwt.getClaim("userId").asString();
            if (jwtUserId != null)
                return ApiResult.ok(jwtUserId);
            else
                return ApiResult.error(Errors.fromPermissionDenied("transfers"));
        } catch (JWTVerificationException | NullPointerException e) {
            return ApiResult.error(Errors.fromUnauthorized());
        }
    }

    private ApiResult<Account> checkPermissions(Account account, String userId) {
        if (Objects.equals(account.getBase64Id(), userId))
            return ApiResult.ok(account);
        return ApiResult.error(Errors.fromPermissionDenied("account"));
    }

    private static class Request {
        private String accountId;
        private String token;
    }
}
