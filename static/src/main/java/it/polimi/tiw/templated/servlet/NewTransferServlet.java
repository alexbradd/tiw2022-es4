package it.polimi.tiw.templated.servlet;

import it.polimi.tiw.api.TransferFacade;
import it.polimi.tiw.api.beans.NewTransferRequest;
import it.polimi.tiw.api.beans.User;
import it.polimi.tiw.api.dbaccess.ProductionConnectionRetriever;
import it.polimi.tiw.api.error.Errors;
import it.polimi.tiw.api.functional.ApiResult;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Endpoint for money transfer. It accepts post requests with the following parameters (form or query string):
 *
 * <ol>
 *     <li>{@code fromAccountId}: source account id (in url-safe base64)</li>
 *     <li>{@code toUserId}: receiving user id (in url-safe base64)</li>
 *     <li>{@code toAccountId}: receiving account id (in url-safe base64)</li>
 *     <li>{@code amount}: the amount to transfer (string representation of a float)</li>
 *     <li>{@code causal}: the causal (a string no longer than 1024 chars)</li>
 * </ol>
 * <p>
 * In case of success, the servlet will redirect to {@code /confirmTransfer} passing the new transfer as parameter.
 * Otherwise, the servlet will redirect to {@code /rejectTransfer} passing the following strings as parameter ({@code e}
 * will be used as parameter name):
 *
 * <ol>
 *     <li>{@code user}: if the parameters were not formatted correctly</li>
 *     <li>{@code missing}: if any resource could not be located (e.g. a user-account combination)</li>
 *     <li>{@code conflict}: if the source account doesn't have enough funds to create the transfer</li>
 *     <li>{@code server}: if the server for some reason could not process the request</li>
 * </ol>
 * <p>
 * This servlet requires a valid {@link HttpSession} with a valid {@link User} saved in it, otherwise it will send
 * a {@code 403 Forbidden} error page.
 */
@WebServlet("/newTransfer")
public class NewTransferServlet extends HttpServlet {
    private Predicate<String> isDecimalFloatingPoint;

    /**
     * {@inheritDoc}
     */
    @Override
    public void init() {
        isDecimalFloatingPoint = Pattern.compile("^[+-]?\\d+(([.,])\\d+)?$").asMatchPredicate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = ServletUtils.tryExtractFromSession(req, "user", User.class);
        if (user == null) {
            resp.sendError(403);
            return;
        }

        String redirect = parseRequest(user, req)
                .flatMap(request -> ProductionConnectionRetriever.getInstance().with(c ->
                        TransferFacade.withDefaultObjects(c).newTransfer(request)))
                .match(t -> "/confirmTransfer.html?id=" + t.getBase64Id(),
                        e -> "/rejectTransfer.html?e=" + switch (e.statusCode()) {
                            case 400 -> "user";
                            case 409 -> "conflict";
                            case 404 -> "missing";
                            default -> "server";
                        });
        resp.sendRedirect(redirect);
    }

    private ApiResult<NewTransferRequest> parseRequest(User user, HttpServletRequest req) {
        NewTransferRequest request = new NewTransferRequest();
        request.setFromUserId(user.getBase64Id());
        request.setFromAccountId(req.getParameter("fromAccountId"));
        request.setToUserId(req.getParameter("toUserId"));
        request.setToAccountId(req.getParameter("toAccountId"));
        request.setCausal(req.getParameter("causal"));

        try {
            String amountString = req.getParameter("amount");
            if (amountString == null)
                return ApiResult.error(Errors.fromNullParameter("amount"));
            if (!isDecimalFloatingPoint.test(amountString))
                return ApiResult.error(Errors.fromMalformedParameter("amount"));
            request.setAmount(Double.parseDouble(amountString));
            return ApiResult.ok(request);
        } catch (NumberFormatException e) {
            return ApiResult.error(Errors.fromMalformedParameter("amount"));
        }
    }
}
