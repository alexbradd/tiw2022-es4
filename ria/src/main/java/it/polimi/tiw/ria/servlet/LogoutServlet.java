package it.polimi.tiw.ria.servlet;

import it.polimi.tiw.api.error.ApiError;
import it.polimi.tiw.api.error.Errors;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;

import static it.polimi.tiw.ria.servlet.ServletUtils.sendJson;

/**
 * Drops all user-related data from the various server-side stores.
 */
@WebServlet("/api/auth/logout")
public class LogoutServlet extends HttpServlet {
    /**
     * {@inheritDoc}
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Cookie[] cookies = req.getCookies();
        if (cookies == null) {
            ApiError e = Errors.fromNullParameter("refresh_cookie");
            sendJson(resp, e.statusCode(), e.toJson());
            return;
        }

        Arrays.stream(cookies).
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
