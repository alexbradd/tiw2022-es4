package it.polimi.tiw.templated.servlet;

import it.polimi.tiw.api.AccountFacade;
import it.polimi.tiw.api.beans.Account;
import it.polimi.tiw.api.beans.User;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Endpoint for creating a new {@link Account} for the {@link User} with the ID specified in the request parameters. It
 * requires a User id encoded in url-safe base64 as a parameter. After creating the new account, it will redirect to
 * {@code index.html}. If an error occurred, it will be signaled to the redirect using the query string parameter
 * {@code e}.
 */
@WebServlet("/createAccount")
public class NewAccountServlet extends HttpServlet {
    /**
     * {@inheritDoc}
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String id = req.getParameter("userId");
        String red;

        if (id == null)
            red = "/index.html?e=user";
        else {
            red = AccountFacade.createFor(id).match(
                    a -> "/index.html",
                    e -> "/index.html?e=" + switch (e.statusCode()) {
                        case 400, 404 -> "user";
                        default -> "server";
                    }
            );
        }
        resp.sendRedirect(red);
    }
}
