package it.polimi.tiw.ria.servlet;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.polimi.tiw.api.TransferFacade;
import it.polimi.tiw.api.beans.InstantTypeAdapter;
import it.polimi.tiw.api.beans.NewTransferRequest;
import it.polimi.tiw.api.beans.Transfer;
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
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static it.polimi.tiw.ria.servlet.ServletUtils.*;
import static java.util.Objects.isNull;

/**
 * This is the endpoint for making transfers. It accepts post requests formatted as such:
 *
 * <ol>
 *     <li>{@code token} the authentication token</li>
 *     <li>{@code fromUserId} the id of the payer user</li>
 *     <li>{@code fromAccountId} the id of the payer's account</li>
 *     <li>{@code toUserId} the id of the payee user</li>
 *     <li>{@code toUserAccountId} the id of the payee's account</li>
 *     <li>{@code causal} the transfer causal</li>
 *     <li>{@code amount} the amount to be transferred</li>
 * </ol>
 * <p>
 * Each field is mandatory. The amount must be greater than 0. The request must provide a valid authentication token
 * or the request will be rejected with a 401. If the user identified by the token is not the payer, the request is
 * refused with a 403. If the payee has insufficient funds a 409 error will be sent.
 * <p>
 * If the operation is a success a JSON object is sent containing the following object in the {@code transfer} field:
 *
 * <pre>
 *     {
 *       "base64Id": ...,
 *       "date": ..., // parsable by Date.parse()
 *       "amount": ...,
 *       "fromId": ...,
 *       "fromBalance": ...,
 *       "toId": ...,
 *       "toBalance": ...,
 *       "causal": ...
 *     }
 * </pre>
 * <p>
 * If an error occurred, a suitable error object is returned.
 */
@WebServlet("/api/transfers")
public class NewTransferServlet extends HttpServlet {
    private Gson gson;
    private String iss, tokenSecret;
    private Predicate<String> isDecimalFloat;

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
        isDecimalFloat = Pattern.compile("^[+-]?\\d+(([.,])\\d+)?$").asMatchPredicate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        TokenWrapper wrapper = new TokenWrapper();
        Tuple<Integer, JsonObject> res =
                checkRequestFormat(gson,
                        req,
                        NewTransferRequest.class,
                        e -> extractToken(removeAmountIfNotDecimal(e), wrapper),
                        this::isRequestInvalid)
                        .flatMap(request -> ProductionConnectionRetriever.getInstance().with(c -> {
                            TransferFacade facade = TransferFacade.withDefaultObjects(c);
                            return checkPermission(request, wrapper.token).flatMap(facade::newTransfer);
                        }))
                        .match(transfer -> {
                                    JsonObject o = new JsonObject();
                                    o.addProperty("type", "OK");
                                    o.add("transfer", gson.toJsonTree(transfer, Transfer.class));
                                    return new Tuple<>(200, o);
                                },
                                err -> new Tuple<>(err.statusCode(), fromApiErrorToJSON(err)));
        sendJson(resp, res.getFirst(), res.getSecond());
    }

    private JsonElement removeAmountIfNotDecimal(JsonElement elem) {
        JsonObject o = elem.getAsJsonObject();
        JsonElement amountElement = o.get("amount");
        if (amountElement != null && amountElement.isJsonPrimitive() && !isDecimalFloat.test(amountElement.getAsString()))
            return null;
        return o;
    }

    private JsonElement extractToken(JsonElement elem, TokenWrapper wrapper) {
        if (elem == null)
            return null;
        wrapper.token = elem.getAsJsonObject().remove("token").getAsString();
        return elem;
    }

    private boolean isRequestInvalid(NewTransferRequest req) {
        return isNull(req.getFromUserId()) ||
                isNull(req.getFromAccountId()) ||
                isNull(req.getToUserId()) ||
                isNull(req.getToAccountId()) ||
                isNull(req.getCausal()) ||
                req.getAmount() <= 0;
    }

    private ApiResult<NewTransferRequest> checkPermission(NewTransferRequest req, String token) {
        try {
            DecodedJWT jwt = AuthUtils.verifyToken(token, iss, tokenSecret);
            String userClaim = jwt.getClaim("userId").asString();
            if (Objects.equals(req.getFromUserId(), userClaim))
                return ApiResult.ok(req);
            else
                return ApiResult.error(Errors.fromPermissionDenied("transfer"));
        } catch (JWTVerificationException | NullPointerException e) {
            return ApiResult.error(Errors.fromUnauthorized());
        }
    }

    private static class TokenWrapper {
        private String token;
    }
}
