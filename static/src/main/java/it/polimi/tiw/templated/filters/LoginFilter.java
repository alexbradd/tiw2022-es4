package it.polimi.tiw.templated.filters;

import it.polimi.tiw.api.beans.User;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Checks if a {@link User} is saved in the session, if there is one. If checks fail, it redirects to the login page
 */
public class LoginFilter extends HttpFilter {
    /**
     * {@inheritDoc}
     */
    @Override
    public void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null)
            response.sendRedirect("/login.html");
        else
            chain.doFilter(request, response);
    }
}
