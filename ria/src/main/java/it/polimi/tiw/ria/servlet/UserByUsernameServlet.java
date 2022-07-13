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

/**
 * Queries the database for a user with the username specified in the {@code username} parameter. If no such user has
 * been found, a 404 error will be sent, otherwise a JSON object containing the id of the User.
 */
@WebServlet("/api/user/byUsername")
public class UserByUsernameServlet extends HttpServlet {
    /**
     * {@inheritDoc}
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String username = req.getParameter("username");
        if (username == null || username.isEmpty()) {
            ServletUtils.sendJson(res, 400, Errors.fromNullParameter("username").toJson());
            return;
        }

        Tuple<Integer, JsonElement> resp = ProductionConnectionRetriever.getInstance()
                .with(c -> UserFacade.withDefaultObjects(c).byUsername(username))
                .match(
                        u -> {
                            JsonObject obj = new JsonObject();
                            obj.addProperty("type", "OK");
                            obj.addProperty("userId", u.getBase64Id());
                            return new Tuple<>(200, obj);
                        },
                        e -> {
                            JsonObject obj = new JsonObject();
                            obj.addProperty("type", "ERROR");
                            obj.add("error", e.toJson());
                            return new Tuple<>(e.statusCode(), obj);
                        }
                );
        ServletUtils.sendJson(res, resp.getFirst(), resp.getSecond());
    }
}
