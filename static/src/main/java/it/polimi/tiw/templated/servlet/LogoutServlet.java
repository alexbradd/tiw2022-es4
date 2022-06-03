package it.polimi.tiw.templated.servlet;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Drops all user-related data from the session, if there is one
 */
@WebServlet("/logout")
public class LogoutServlet extends HttpServlet {
    /**
     * {@inheritDoc}
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null)
            return;
        session.invalidate();
        resp.sendRedirect("/login.html");
    }
}
