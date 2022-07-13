package it.polimi.tiw.templated.filters;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Sets the "currentlyShownAccount" attribute of the session to the value of the "id" query parameter. If either the
 * session or the parameter is not present, nothing is done.
 */
public class UpdateCurrentlyShownAccountFilter extends HttpFilter {
    /**
     * {@inheritDoc}
     */
    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpSession session = req.getSession(false);
        String id = req.getParameter("id");
        if (session != null && id != null)
            session.setAttribute("currentlyShownAccount", id);
        chain.doFilter(req, res);
    }
}
