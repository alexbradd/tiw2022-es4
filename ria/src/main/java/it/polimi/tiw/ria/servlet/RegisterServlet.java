package it.polimi.tiw.ria.servlet;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonReader;
import it.polimi.tiw.api.UserFacade;
import it.polimi.tiw.api.beans.RegistrationRequest;
import it.polimi.tiw.api.dbaccess.ProductionConnectionRetriever;
import it.polimi.tiw.api.error.ApiError;
import it.polimi.tiw.api.functional.Tuple;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static it.polimi.tiw.ria.servlet.ServletUtils.*;

/**
 * Endpoint for user registration. It accepts POST requests containing a JSON object with the following fields:
 *
 * <ul>
 *     <li>'username': the username of the new user</li>
 *     <li>'clearPassword': the cleartext password of the new user</li>
 *     <li>'repeatPassword': a repetition of the cleartext password of the new user</li>
 *     <li>'email':  the email of the new user</li>
 *     <li>'name':  the name of the new user</li>
 *     <li>'surname':  the surname of the new user</li>
 * </ul>
 * <p>
 * All fields are mandatory and have a maximum length of 128 characters. In case of success, the servlet will
 * send a 200 OK message. Otherwise, it will send a response with a 4xx error code and a JSON object describing the
 * error.
 *
 * @see ApiError
 */
@WebServlet("/api/users")
public class RegisterServlet extends HttpServlet {
    /**
     * {@inheritDoc}
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (hasNotJSONContentType(req)) {
            sendWrongTypeError(resp);
            return;
        }

        JsonReader jsonReader = new JsonReader(req.getReader());
        try {
            RegistrationRequest registrationReq = new Gson().fromJson(jsonReader, RegistrationRequest.class);
            Tuple<Integer, JsonObject> res = ProductionConnectionRetriever.getInstance()
                    .with(c -> UserFacade.withDefaultObjects(c).register(registrationReq))
                    .match((u) -> {
                                JsonObject obj = new JsonObject();
                                obj.addProperty("type", "OK");
                                return new Tuple<>(200, obj);
                            },
                            (e) -> new Tuple<>(e.statusCode(), fromApiErrorToJSON(e)));
            sendJson(resp, res.getFirst(), res.getSecond());
        } catch (JsonParseException e) {
            sendInvalidFormatError(resp);
        }
    }
}
