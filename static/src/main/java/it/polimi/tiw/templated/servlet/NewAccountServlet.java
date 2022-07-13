package it.polimi.tiw.templated.servlet;

import it.polimi.tiw.api.AccountFacade;
import it.polimi.tiw.api.beans.Account;
import it.polimi.tiw.api.beans.User;
import it.polimi.tiw.api.dbaccess.ProductionConnectionRetriever;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Endpoint for creating a new {@link Account} for the currently logged in {@link User}. It requires a Session with a
 * valid user. If such requirements are not met, the request will be rejected with a 403.
 * <p>
 * After creating the new account, it will redirect to {@code index.html}. If an error occurred, it will be signaled to
 * the redirect using the query string parameter.
 */
@WebServlet("/createAccount")
public class NewAccountServlet extends HttpServlet {
    /**
     * {@inheritDoc}
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User u = ServletUtils.tryExtractFromSession(req, "user", User.class);
        if (u == null) {
            resp.sendError(403);
            return;
        }

        String red = ProductionConnectionRetriever.getInstance()
                .with(c -> AccountFacade.withDefaultObjects(c).createFor(u.getBase64Id()))
                .match(
                        a -> "/index.html",
                        e -> "/index.html?e=" + switch (e.statusCode()) {
                            case 400, 404 -> "user";
                            default -> "server";
                        }
                );
        resp.sendRedirect(red);
    }
}
