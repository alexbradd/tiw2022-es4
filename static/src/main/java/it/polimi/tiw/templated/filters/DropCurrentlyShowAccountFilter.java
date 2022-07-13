package it.polimi.tiw.templated.filters;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Removes the "currentlyShownAccount" property from the session, if there is one
 */
public class DropCurrentlyShowAccountFilter extends HttpFilter {
    /**
     * {@inheritDoc}
     */
    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpSession session = req.getSession(false);
        if (session != null)
            session.removeAttribute("currentlyShownAccount");
        chain.doFilter(req, res);
    }
}
