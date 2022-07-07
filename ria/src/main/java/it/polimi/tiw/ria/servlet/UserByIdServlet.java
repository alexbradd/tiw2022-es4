package it.polimi.tiw.ria.servlet;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.polimi.tiw.api.UserFacade;
import it.polimi.tiw.api.dbaccess.ProductionConnectionRetriever;
import it.polimi.tiw.api.error.Errors;
import it.polimi.tiw.api.functional.Tuple;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static it.polimi.tiw.ria.servlet.ServletUtils.fromApiErrorToJSON;
import static it.polimi.tiw.ria.servlet.ServletUtils.sendJson;

/**
 * Queries the database for a user with the id specified in the {@code id} parameter. If no such user has
 * been found, a 404 error will be sent, otherwise a JSON object containing the id of the User.
 */
@WebServlet("/api/user/byId")
public class UserByIdServlet extends HttpServlet {
    /**
     * {@inheritDoc}
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String id = req.getParameter("id");
        if (id == null || id.isEmpty()) {
            sendJson(res, 400, Errors.fromMalformedParameter("id").toJson());
            return;
        }

        Tuple<Integer, JsonElement> resp = ProductionConnectionRetriever.getInstance()
                .with(c -> UserFacade.withDefaultObjects(c).byId(id))
                .match(u -> {
                            JsonObject obj = new JsonObject();
                            obj.addProperty("type", "OK");
                            obj.addProperty("userId", u.getBase64Id());
                            return new Tuple<>(200, obj);
                        },
                        e -> new Tuple<>(e.statusCode(), fromApiErrorToJSON(e)));
        sendJson(res, resp.getFirst(), resp.getSecond());
    }
}
