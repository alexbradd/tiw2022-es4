package it.polimi.tiw.api;

import it.polimi.tiw.api.beans.NewTransferRequest;
import it.polimi.tiw.api.beans.Transfer;
import it.polimi.tiw.api.dbaccess.AccountDAO;
import it.polimi.tiw.api.dbaccess.TransferDAO;
import it.polimi.tiw.api.error.Errors;
import it.polimi.tiw.api.functional.ApiResult;
import it.polimi.tiw.api.functional.Tuple;

import java.sql.Connection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Class exposing simple commands for working with transfers
 */
public class TransferFacade {
    private final Connection connection;
    private final Function<Connection, TransferDAO> transferDAOGenerator;
    private final Function<Connection, AccountFacade> accountFacadeGenerator;

    public TransferFacade(Connection connection,
                          Function<Connection, TransferDAO> transferDAOGenerator,
                          Function<Connection, AccountFacade> accountFacadeGenerator) {
        this.connection = Objects.requireNonNull(connection);
        this.transferDAOGenerator = Objects.requireNonNull(transferDAOGenerator);
        this.accountFacadeGenerator = Objects.requireNonNull(accountFacadeGenerator);
    }

    /**
     * Returns the {@link Transfer} with the given base64 encoded id.
     *
     * @param id the id of the Transfer to get
     * @return an {@link ApiResult} containing the Transfer searched or an error
     * @see TransferDAO#byId(String)
     */
    public ApiResult<Transfer> byId(String id) {
        return transferDAOGenerator.apply(connection).byId(id);
    }

    /**
     * Checks the validity of the given {@link NewTransferRequest} and executes it.
     *
     * @param transferRequest the request to evaluate
     * @return an {@link ApiResult} containing the newly created {@link Transfer} or an error
     * @see TransferDAO#newTransfer(String, String, double, String)
     */
    public ApiResult<Transfer> newTransfer(NewTransferRequest transferRequest) {
        if (transferRequest == null) return ApiResult.error(Errors.fromNullParameter("transferRequest"));
        AccountFacade facade = accountFacadeGenerator.apply(connection);
        return checkAccountOwnership(facade,
                transferRequest.getFromUserId(),
                transferRequest.getFromAccountId(),
                "fromAccountId")
                .flatMap((f) -> checkAccountOwnership(f,
                        transferRequest.getToUserId(),
                        transferRequest.getToAccountId(),
                        "toAccountId"))
                .then(() -> transferDAOGenerator.apply(connection).newTransfer(
                        transferRequest.getFromAccountId(),
                        transferRequest.getToAccountId(),
                        transferRequest.getAmount(),
                        transferRequest.getCausal()));
    }

    private ApiResult<AccountFacade> checkAccountOwnership(AccountFacade facade,
                                                           String userId,
                                                           String accountId,
                                                           String accountParamName) {
        if (accountId == null || accountId.isEmpty())
            return ApiResult.error(Errors.fromMalformedParameter(accountParamName));
        return facade.ofUser(userId)
                .flatMap(list -> {
                    if (list.stream().anyMatch(a -> a.getBase64Id().equals(accountId)))
                        return ApiResult.ok(facade);
                    return ApiResult.error(Errors.fromNotFound(accountParamName));
                });
    }

    /**
     * Returns the incoming and outgoing transfers of the account with the given id.
     *
     * @param accountId the id of the account to query
     * @return and {@link ApiResult} containing a {@link Tuple} with, in order, the incoming and outgoing transfers or
     * an error
     */
    public ApiResult<Tuple<List<Transfer>, List<Transfer>>> of(String accountId) {
        return transferDAOGenerator.apply(connection).inAndOutOf(accountId);
    }

    /**
     * Creates a new TransferFacade using the default objects
     *
     * @param connection The {@link Connection} to use
     * @return a new TransferFacade
     */
    public static TransferFacade withDefaultObjects(Connection connection) {
        return new TransferFacade(connection,
                (c) -> new TransferDAO(c, new AccountDAO(c)),
                AccountFacade::withDefaultObjects);
    }

}
