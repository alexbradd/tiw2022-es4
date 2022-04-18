package it.polimi.tiw.api.servlet;

import it.polimi.tiw.api.dbaccess.User;

import javax.json.Json;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

import static it.polimi.tiw.api.servlet.ServletUtils.constructError;
import static it.polimi.tiw.api.servlet.ServletUtils.executeLogicAndHandleErrors;

/**
 * Endpoint for querying the user list. It supports two types of searches: by id and by username. The query term is
 * passed via the {@code q} parameter (either querystring of form data) and it needs to be url friendly. If the query
 * parameter is passed both in the query string and in the request body, only one of them will be used.
 * <p>
 * In case a user with the given properties is not found, a 404 is returned. If the request is formatted improperly, a
 * 400 is returned with the standard json error message constructed by
 * {@link ServletUtils#constructError(String, String)}. If everything goes well, a json containing the properties of
 * a User the requester can see is returned.
 */
@WebServlet(urlPatterns = {"/userinfo/byId", "/userinfo/byUsername"})
public class UserInfoServlet extends HttpServlet {

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");

        String argument = req.getParameter("q");

        executeLogicAndHandleErrors(req, resp, () -> {
            Optional<User> found;
            if (req.getServletPath().equals("/userinfo/byId"))
                found = User.byId(argument);
            else
                found = User.byUsername(argument);
            resp.getWriter().print(
                    found.map(u -> Json.createObjectBuilder()
                                    .add("id", u.getId().orElseThrow(IllegalStateException::new))
                                    .add("username", u.getUsername())
                                    .add("email", u.getEmail())
                                    .add("name", u.getName())
                                    .add("surname", u.getSurname())
                                    .build())
                            .orElseGet(() -> {
                                resp.setStatus(404);
                                return constructError("Could not find user", argument);
                            }));

        });
    }
}
