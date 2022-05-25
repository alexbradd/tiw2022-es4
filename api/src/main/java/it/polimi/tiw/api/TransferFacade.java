package it.polimi.tiw.api;

import it.polimi.tiw.api.beans.Transfer;
import it.polimi.tiw.api.dbaccess.AccountDAO;
import it.polimi.tiw.api.dbaccess.TransferDAO;
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

    public TransferFacade(Connection connection, Function<Connection, TransferDAO> transferDAOGenerator) {
        this.connection = Objects.requireNonNull(connection);
        this.transferDAOGenerator = Objects.requireNonNull(transferDAOGenerator);
    }

    /**
     * Returns the {@link Transfer} with the given base64 encoded id.
     *
     * @param id the id of the Transfer to get
     * @return an {@link ApiResult} containing the Transfer searched or an error
     * @see it.polimi.tiw.api.dbaccess.TransferDAO#byId(String)
     */
    public ApiResult<Transfer> byId(String id) {
        return transferDAOGenerator.apply(connection).byId(id);
    }

    /**
     * Creates a new transfer from the accounts with the given ids.
     *
     * @param from   the id of the account from which the money will be taken
     * @param to     the id of the account to which the money will be given
     * @param amount the amount of money to take
     * @param causal the causal message
     * @return an {@link ApiResult} containing the newly created Transfer or an error
     * @see it.polimi.tiw.api.dbaccess.TransferDAO#newTransfer(String, String, int, String)
     */
    public ApiResult<Transfer> newTransfer(String from, String to, int amount, String causal) {
        return transferDAOGenerator.apply(connection).newTransfer(from, to, amount, causal);
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
        return new TransferFacade(connection, (c) -> new TransferDAO(c, new AccountDAO(c)));
    }

}
