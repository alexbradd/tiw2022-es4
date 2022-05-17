package it.polimi.tiw.templated.servlet;


import it.polimi.tiw.api.UserFacade;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Endpoint for user registration. It accepts POST requests and forwards them to
 * {@link UserFacade#register(HttpServletRequest)}. In case of success, the servlet will redirect to the templated login
 * page. Otherwise, it will redirect to the register page with an error flag in the query string: "e". The various
 * values for "e" are:
 *
 * <ol>
 *     <li>"user": if the form contained errors (parameters missing or malformed)</li>
 *     <li>"conflict": if there is already an user with the same username</li>
 *     <li>"server": if the server was unable to fulfill the request (e.g. database is unavailable)</li>
 * </ol>
 */
@WebServlet("/registerUser")
public class RegistrationServlet extends HttpServlet {
    /**
     * {@inheritDoc}
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String redirect = UserFacade.withDefaultObjects()
                .register(req)
                .match((u) -> "/login.html",
                        (e) -> "/register.html?e=" + switch (e.statusCode()) {
                            case 400 -> "user";
                            case 409 -> "conflict";
                            default -> "server";
                        });
        resp.sendRedirect(redirect);
    }
}
