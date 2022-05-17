package it.polimi.tiw.templated.filters;

import it.polimi.tiw.api.AccountFacade;
import it.polimi.tiw.api.beans.Account;
import it.polimi.tiw.api.beans.User;
import it.polimi.tiw.api.functional.ApiResult;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

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
        ApiResult<List<Account>> result = AccountFacade.ofUser(user);
        if (result.match(__ -> true, __ -> false)) {
            List<Account> l = result.get();
            req.setAttribute("accountList", l);
        } else {
            req.setAttribute("accountList", null);
        }
        chain.doFilter(req, res);
    }
}
