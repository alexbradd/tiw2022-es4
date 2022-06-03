package it.polimi.tiw.ria.servlet;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;

/**
 * Drops all user-related data from the various server-side stores.
 */
@WebServlet("/api/auth/logout")
public class LogoutServlet extends HttpServlet {
    /**
     * {@inheritDoc}
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        Arrays.stream(req.getCookies()).
                filter(c -> c.getName().equals("refresh"))
                .findAny()
                .map(c -> {
                    c.setValue("");
                    c.setMaxAge(0);
                    return c;
                })
                .ifPresent(resp::addCookie);
    }
}
