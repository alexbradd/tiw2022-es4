package it.polimi.tiw.ria.servlet;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import it.polimi.tiw.api.ContactFacade;
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
import java.util.Objects;

import static it.polimi.tiw.ria.servlet.ServletUtils.*;
import static java.util.Objects.isNull;

/**
 * Endpoint for querying the contacts of a user. It accepts POST requests containing a JSON object with the following
 * properties:
 *
 * <ol>
 *     <li>{@code token} the authorization token</li>
 *     <li>{@code userId} the base64 id of the user of which the contacts will be returned</li>
 * </ol>
 * <p>
 * The request must provide a valid authorization token or the request will be rejected with a 401. Moreover, the
 * token must be associated with the user of which the contacts are being queried, or the response will be rejected with
 * 403.
 * <p>
 * The response will contain JSON object containing an array of objects in its {@code contacts} properties. The objects
 * are formatted as such:
 *
 * <pre>
 *     {
 *       "ownerBase64Id": ...,
 *       "contactBase64Id": ...,
 *     }
 * </pre>
 * <p>
 * If any error has been encountered, a suitable JSON object describing the error is attached in the response.
 */
@WebServlet("/api/contacts/ofUser")
public class ContactsOfUserServlet extends HttpServlet {
    private Gson gson;
    private String iss, tokenSecret;

    /**
     * {@inheritDoc}
     */
    @Override
    public void init() {
        iss = getServletContext().getInitParameter("ISSUER");
        tokenSecret = getServletContext().getInitParameter("TOKEN_SECRET");
        gson = new Gson();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Tuple<Integer, JsonObject> res =
                checkRequestFormat(gson, req, Request.class, r -> isNull(r.userId))
                        .flatMap(request -> ProductionConnectionRetriever.getInstance().with(c -> {
                            ContactFacade facade = ContactFacade.withDefaultObjects(c);
                            return checkPermissions(request).flatMap(id -> facade.ofUser(request.userId));
                        }))
                        .match(contactList -> {
                                    JsonObject obj = new JsonObject();
                                    JsonArray contactArray = listToJsonArray(gson, contactList);
                                    obj.addProperty("type", "OK");
                                    obj.add("contacts", contactArray);
                                    return new Tuple<>(200, obj);
                                },
                                err -> new Tuple<>(err.statusCode(), fromApiErrorToJSON(err)));
        sendJson(resp, res.getFirst(), res.getSecond());
    }

    private ApiResult<String> checkPermissions(Request request) {
        try {
            DecodedJWT jwt = AuthUtils.verifyToken(request.token, iss, tokenSecret);
            String userClaim = jwt.getClaim("userId").asString();
            if (Objects.equals(userClaim, request.userId))
                return ApiResult.ok(userClaim);
            else
                return ApiResult.error(Errors.fromPermissionDenied("contacts"));
        } catch (JWTVerificationException | NullPointerException e) {
            return ApiResult.error(Errors.fromUnauthorized());
        }
    }

    private static class Request {
        private String userId, token;
    }
}
