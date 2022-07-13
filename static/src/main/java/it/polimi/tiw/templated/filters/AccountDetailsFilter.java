package it.polimi.tiw.templated.filters;

import it.polimi.tiw.api.AccountFacade;
import it.polimi.tiw.api.TransferFacade;
import it.polimi.tiw.api.beans.Account;
import it.polimi.tiw.api.beans.Transfer;
import it.polimi.tiw.api.beans.User;
import it.polimi.tiw.api.dbaccess.ProductionConnectionRetriever;
import it.polimi.tiw.api.error.ApiError;
import it.polimi.tiw.api.error.Errors;
import it.polimi.tiw.api.functional.Tuple;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * Adds the following attributes to request attributes:
 *
 * <ol>
 *     <li>{@code account} the account with id specified by the {@code id} parameter</li>
 *     <li>{@code incoming} the list of incoming transfers to {@code account}</li>
 *     <li>{@code outgoing} the list of outgoing transfers from {@code account}</li>
 * </ol>
 * <p>
 * If the User stored inside the session is not the owner of the account, an {@link ApiError} is set as {@code error}.
 * <p>
 * Requires being executed after {@link LoginFilter}.
 */
public class AccountDetailsFilter extends HttpFilter {
    /**
     * {@inheritDoc}
     */
    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException, ServletException {
        String accountId = req.getParameter("id");
        if (accountId == null) {
            req.setAttribute("error", Errors.fromNullParameter("id"));
            chain.doFilter(req, res);
            return;
        }

        HttpSession session = req.getSession(false);
        User user = (User) session.getAttribute("user");
        ApiError unavailable = Errors.fromPermissionDenied("account(" + accountId + ")");
        ProductionConnectionRetriever.getInstance()
                .with(c -> AccountFacade.withDefaultObjects(c)
                        .byId(accountId)
                        .flatMap(a -> TransferFacade.withDefaultObjects(c)
                                .of(a.getBase64Id())
                                .map(t -> new Tuple<>(a, t))))
                .consume(
                        tuple -> {
                            Account account = tuple.getFirst();
                            List<Transfer> incoming = tuple.getSecond().getFirst();
                            List<Transfer> outgoing = tuple.getSecond().getSecond();
                            if (Objects.equals(account.getOwnerId(), user.getBase64Id())) {
                                req.setAttribute("account", account);
                                req.setAttribute("incoming", incoming);
                                req.setAttribute("outgoing", outgoing);
                            } else
                                req.setAttribute("error", unavailable);
                        },
                        e -> req.setAttribute("error", e)
                );
        chain.doFilter(req, res);
    }
}
