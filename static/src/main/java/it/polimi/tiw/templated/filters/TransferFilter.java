package it.polimi.tiw.templated.filters;

import it.polimi.tiw.api.AccountFacade;
import it.polimi.tiw.api.TransferFacade;
import it.polimi.tiw.api.beans.Transfer;
import it.polimi.tiw.api.beans.User;
import it.polimi.tiw.api.dbaccess.ProductionConnectionRetriever;
import it.polimi.tiw.api.error.ApiError;
import it.polimi.tiw.api.error.Errors;
import it.polimi.tiw.api.functional.ApiResult;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Retrieves a {@link Transfer} with the id specified in the {@code id} parameter and stores it in the {@code transfer}
 * attribute of the request. If the User is not the owner of the payer account, access to the resource is denied and a
 * 403 {@link ApiError} is set to the {@code error} attribute.
 * <p>
 * It requires being executed after {@link LoginFilter}
 */
public class TransferFilter extends HttpFilter {
    /**
     * {@inheritDoc}
     */
    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException, ServletException {
        String transferId = req.getParameter("id");
        if (transferId == null) {
            req.setAttribute("error", Errors.fromNullParameter("id"));
            chain.doFilter(req, res);
            return;
        }

        HttpSession session = req.getSession(false);
        User user = (User) session.getAttribute("user");
        ApiError unavailable = Errors.fromPermissionDenied("transfer(" + transferId + ")");
        ProductionConnectionRetriever.getInstance()
                .with(c -> TransferFacade.withDefaultObjects(c)
                        .byId(transferId)
                        .flatMap(t -> {
                            AccountFacade facade = AccountFacade.withDefaultObjects(c);
                            return checkAccountOwnership(facade, t.getFromId(), user.getBase64Id(), unavailable)
                                    .then(() -> ApiResult.ok(t));
                        }))
                .consume(
                        t -> req.setAttribute("transfer", t),
                        e -> req.setAttribute("error", e));
        chain.doFilter(req, res);
    }

    private ApiResult<AccountFacade> checkAccountOwnership(AccountFacade facade,
                                                           String accountId,
                                                           String ownerId,
                                                           ApiError unavailable) {
        return facade.byId(accountId)
                .flatMap(a -> {
                    if (!a.getOwnerId().equals(ownerId))
                        return ApiResult.error(unavailable);
                    return ApiResult.ok(facade);
                });
    }
}
