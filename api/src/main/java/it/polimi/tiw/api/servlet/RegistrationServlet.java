package it.polimi.tiw.api.servlet;

import it.polimi.tiw.api.dbaccess.User;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static it.polimi.tiw.api.servlet.ServletUtils.*;

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
 * All fields are mandatory, must be urlencoded and have a maximum length of 128 characters. If data is passed in both
 * ways (querystring and form data) only one of the two sources will be used. In case of success, the servlet will
 * respond with a 200, otherwise a 40x and an error JSON object
 * (see {@link ServletUtils#constructError(String, String)}) is returned.
 */
@WebServlet("/register")
public class RegistrationServlet extends HttpServlet {

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");

        String username = req.getParameter("username");
        String password1 = req.getParameter("clearPassword");
        String password2 = req.getParameter("repeatPassword");
        String email = req.getParameter("email");
        String name = req.getParameter("name");
        String surname = req.getParameter("surname");

        executeLogicAndHandleErrors(req, resp, () -> {
            if (!password1.equals(password2))
                setStatusAndWriteError(resp,
                        400,
                        constructError("Passwords do not match", password1 + " ::: " + password2));
            else if (User.byUsername(username).isPresent())
                setStatusAndWriteError(resp,
                        409,
                        constructError("Username already exists", username));
            else
                new User(username, password1, email, name, surname).save();
        });
    }
}
