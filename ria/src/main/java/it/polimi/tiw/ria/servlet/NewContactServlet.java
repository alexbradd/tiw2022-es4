package it.polimi.tiw.ria.servlet;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.Gson;
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

import static it.polimi.tiw.ria.servlet.ServletUtils.*;
import static java.util.Objects.isNull;

/**
 * Endpoint for creating a new contact for the currently logged-in user. It accepts POST requests containing a JSON
 * object formatted as such:
 *
 * <ol>
 *     <li>{@code token} the authorization token</li>
 *     <li>{@code contactId} the is of the user in the contact</li>
 * </ol>
 * <p>
 * The request must provide a valid authentication token or the request will be rejected with a 401. If the operation
 * was a success, then a JSON object containing the {@code type} property set to {@code OK} will be sent in a 200
 * response. Otherwise, a suitable error object is returned in a response with suitable code.
 */
@WebServlet("/api/contacts")
public class NewContactServlet extends HttpServlet {
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
                checkRequestFormat(gson, req, Request.class, r -> isNull(r.contactId))
                        .flatMap(request -> ProductionConnectionRetriever.getInstance().with(c -> {
                            ContactFacade facade = ContactFacade.withDefaultObjects(c);
                            return checkPermissions(request)
                                    .flatMap(id -> facade.saveContact(id, request.contactId));
                        }))
                        .match(contact -> {
                                    JsonObject obj = new JsonObject();
                                    obj.addProperty("type", "OK");
                                    return new Tuple<>(200, obj);
                                },
                                err -> new Tuple<>(err.statusCode(), fromApiErrorToJSON(err)));
        sendJson(resp, res.getFirst(), res.getSecond());
    }

    private ApiResult<String> checkPermissions(Request request) {
        try {
            DecodedJWT jwt = AuthUtils.verifyToken(request.token, iss, tokenSecret);
            String userClaim = jwt.getClaim("userId").asString();
            if (!isNull(userClaim))
                return ApiResult.ok(userClaim);
            else
                return ApiResult.error(Errors.fromPermissionDenied("contacts"));
        } catch (JWTVerificationException | NullPointerException e) {
            return ApiResult.error(Errors.fromUnauthorized());
        }
    }

    private static class Request {
        private String contactId, token;
    }
}
