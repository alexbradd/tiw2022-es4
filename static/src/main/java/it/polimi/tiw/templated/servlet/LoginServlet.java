package it.polimi.tiw.templated.servlet;

import it.polimi.tiw.api.UserFacade;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Endpoint for user login. It accepts POST requests and forwards them to
 * {@link UserFacade#authorize(HttpServletRequest)}. In case of success, the servlet will redirect to the templated
 * home page. Otherwise, it will redirect to the login page with an error flag in the query string: "e". The various
 * values for "e" are:
 *
 * <ol>
 *     <li>"user": if the form contained errors (parameters missing or malformed)</li>
 *     <li>"conflict": if the combination of username + password is incorrect</li>
 *     <li>"server": if the server was unable to fulfill the request (e.g. database is unavailable)</li>
 * </ol>
 * <p>
 * If the user is correctly authenticated, a session will be created for tracking this user.
 */
@WebServlet("/loginUser")
public class LoginServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String redirect = UserFacade.authorize(req).match(u -> {
            HttpSession session = req.getSession(true);
            session.setAttribute("user", u);
            return "/index.html";
        }, e -> switch (e.statusCode()) {
            case 400 -> "/login.html?e=user";
            case 404, 409 -> "/login.html?e=conflict";
            default -> "/login.html?e=server";
        });
        resp.sendRedirect(redirect);
    }
}
