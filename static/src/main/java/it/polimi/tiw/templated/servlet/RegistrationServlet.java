package it.polimi.tiw.templated.servlet;

import it.polimi.tiw.api.UserFacade;
import it.polimi.tiw.api.beans.RegistrationRequest;
import it.polimi.tiw.api.dbaccess.ProductionConnectionRetriever;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Endpoint for user registration. It accepts POST requests with the following fields (as either query parameters or
 * form data):
 *
 * <ul>
 *     <li>Field 'username': the username of the new user</li>
 *     <li>Field 'clearPassword': the cleartext password of the new user</li>
 *     <li>Field 'repeatPassword': a repetition of the cleartext password of the new user</li>
 *     <li>Field 'email':  the email of the new user</li>
 *     <li>Field 'name':  the name of the new user</li>
 *     <li>Field 'surname':  the surname of the new user</li>
 * </ul>
 * <p>
 * All fields are mandatory, must be urlencoded and have a maximum length of 128 characters. If data is passed in
 * both ways (querystring and form data) only one of the two sources will be used. In case of success, the servlet will
 * redirect to the templated login page. Otherwise, it will redirect to the register page with an error flag in the
 * query string: "e". The various values for "e" are:
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
        RegistrationRequest registrationReq = new RegistrationRequest();
        registrationReq.setUsername(req.getParameter("username"));
        registrationReq.setClearPassword(req.getParameter("clearPassword"));
        registrationReq.setRepeatPassword(req.getParameter("repeatPassword"));
        registrationReq.setEmail(req.getParameter("email"));
        registrationReq.setName(req.getParameter("name"));
        registrationReq.setSurname(req.getParameter("surname"));

        String redirect = ProductionConnectionRetriever.getInstance()
                .with(c -> UserFacade.withDefaultObjects(c).register(registrationReq))
                .match((u) -> "/login.html",
                        (e) -> "/register.html?e=" + switch (e.statusCode()) {
                            case 400 -> "user";
                            case 409 -> "conflict";
                            default -> "server";
                        });
        resp.sendRedirect(redirect);
    }
}
