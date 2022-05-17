package it.polimi.tiw.templated.filters;

import it.polimi.tiw.api.AccountFacade;
import it.polimi.tiw.api.beans.Account;
import it.polimi.tiw.api.beans.User;
import it.polimi.tiw.api.dbaccess.ProductionConnectionRetriever;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Adds as the 'accountList' attributes the list of all the {@link Account} associated with the {@link User} stored
 * inside the session. Requires being executed after {@link LoginFilter}.
 */
public class AccountListFilter extends HttpFilter {
    /**
     * {@inheritDoc}
     */
    @Override
    public void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpSession session = req.getSession(false);
        User user = (User) session.getAttribute("user");
        ProductionConnectionRetriever.getInstance()
                .with(c -> AccountFacade.withDefaultObjects(c).ofUser(user))
                .consume(
                        l -> req.setAttribute("accountList", l),
                        e -> req.setAttribute("accountList", null));
        chain.doFilter(req, res);
    }
}
