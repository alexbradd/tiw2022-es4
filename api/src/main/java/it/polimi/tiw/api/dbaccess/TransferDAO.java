package it.polimi.tiw.api.dbaccess;

import it.polimi.tiw.api.beans.Account;
import it.polimi.tiw.api.beans.Transfer;
import it.polimi.tiw.api.error.ApiError;
import it.polimi.tiw.api.error.ApiSubError;
import it.polimi.tiw.api.error.Errors;
import it.polimi.tiw.api.functional.ApiResult;
import it.polimi.tiw.api.functional.Result;
import it.polimi.tiw.api.functional.Tuple;
import it.polimi.tiw.api.utils.IdUtils;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

/**
 * Class for retrieving/sending {@link Transfer} instances to a database.
 */
public class TransferDAO implements DatabaseAccessObject<Transfer> {
    private final Connection connection;
    private final AccountDAO accountDAO;

    /**
     * Instantiates a new TransferDAO using the given {@link Connection} and {@link AccountDAO}.
     *
     * @param connection the {@link Connection} to use.
     * @param accountDAO the {@link AccountDAO} to use
     * @throws NullPointerException if any parameter is null
     */
    public TransferDAO(Connection connection, AccountDAO accountDAO) {
        requireNonNull(connection);
        requireNonNull(accountDAO);
        this.connection = connection;
        this.accountDAO = accountDAO;
    }

    /**
     * Finds and retrieves the data for the {@link Transfer} with the given id. If no such object can be found, an empty
     * {@link ApiResult} is returned.
     *
     * @param base64Id the id to search
     * @return an {@link ApiResult} containing the constructed object
     */
    @Override
    public ApiResult<Transfer> byId(String base64Id) {
        if (isNull(base64Id)) return ApiResult.error(Errors.fromNullParameter("base64Id"));
        if (!IdUtils.isValidBase64(base64Id)) return ApiResult.error(Errors.fromMalformedParameter("base64Id"));

        long id = IdUtils.fromBase64(base64Id);
        try (PreparedStatement p = connection.prepareStatement("select * from tiw_app.transfers where id = ?")) {
            p.setLong(1, id);
            try (ResultSet r = p.executeQuery()) {
                if (r.next()) {
                    Instant date = r.getTimestamp("date").toInstant();
                    double amount = r.getDouble("amount");
                    long toId = r.getLong("toId");
                    double toBalance = r.getDouble("toBalance");
                    long fromId = r.getLong("fromId");
                    double fromBalance = r.getDouble("fromBalance");
                    String causal = r.getString("causal");
                    Transfer t = new Transfer();
                    t.setDate(date);
                    t.setAmount(amount);
                    t.setBase64Id(base64Id);
                    t.setToId(IdUtils.toBase64(toId));
                    t.setToBalance(toBalance);
                    t.setFromId(IdUtils.toBase64(fromId));
                    t.setFromBalance(fromBalance);
                    t.setCausal(causal);
                    return ApiResult.ok(t);
                } else
                    return ApiResult.error(Errors.fromNotFound("id " + base64Id));
            }
        } catch (SQLException e) {
            return ApiResult.error(Errors.fromSQLException(e));
        }
    }

    /**
     * Finds and retrieves all {@link Transfer}s relative to the {@link Account} with the given id. Each
     * {@link Transfer} set is returned as a {@link List} ordered by descending date. The first element of the tuple are
     * the transfers the {@link Account} received, the latter are those sent.
     *
     * @param accountId the {@link Account} of which to get the transfers
     * @return an {@link ApiResult} containing the tuple with the transfers or an error if something went wrong.
     */
    public ApiResult<Tuple<List<Transfer>, List<Transfer>>> inAndOutOf(String accountId) {
        if (isNull(accountId)) return ApiResult.error(Errors.fromNullParameter("account"));
        if (!IdUtils.isValidBase64(accountId))
            return ApiResult.error(Errors.fromMalformedParameter("account"));

        long id = IdUtils.fromBase64(accountId);
        try {
            ArrayList<Transfer> ins = new ArrayList<>();
            ArrayList<Transfer> outs = new ArrayList<>();
            String sql = "select * from tiw_app.transfers where toId = ? or fromId = ? order by date desc";
            try (PreparedStatement s = connection.prepareStatement(sql)) {
                s.setLong(1, id);
                s.setLong(2, id);
                try (ResultSet r = s.executeQuery()) {
                    while (r.next()) {
                        Transfer t = new Transfer();
                        t.setBase64Id(IdUtils.toBase64(r.getLong("id")));
                        t.setDate(r.getTimestamp("date").toInstant());
                        t.setAmount(r.getDouble("amount"));
                        t.setToId(IdUtils.toBase64(r.getLong("toId")));
                        t.setToBalance(r.getDouble("toBalance"));
                        t.setFromId(IdUtils.toBase64(r.getLong("fromId")));
                        t.setFromBalance(r.getDouble("fromBalance"));
                        t.setCausal(r.getString("causal"));
                        if (t.getToId().equals(accountId)) ins.add(t);
                        else outs.add(t);
                    }
                    return ApiResult.ok(new Tuple<>(ins, outs));
                }
            }
        } catch (SQLException e) {
            return ApiResult.error(Errors.fromSQLException(e));
        }
    }

    /**
     * Creates a new {@link Transfer} between two {@link Account} with the given ids anda the specified amount. If a
     * {@link Transfer} could be created, it is returned in an {@link ApiResult}, otherwise an error is returned.
     *
     * @param fromId the base64 encoded id of the {@link Account} from which the money will be taken
     * @param toId   the base64 encoded id of the {@link Account} on which the money will be deposited
     * @param amount the amount of money transferred
     * @param causal the causal message
     * @return an {@link ApiResult} containing the created {@link Transfer} or an error.
     */
    public ApiResult<Transfer> newTransfer(String fromId, String toId, double amount, String causal) {
        if (isNull(fromId)) return ApiResult.error(Errors.fromNullParameter("fromId"));
        if (isNull(toId)) return ApiResult.error(Errors.fromNullParameter("toId"));
        if (isNull(causal)) return ApiResult.error(Errors.fromNullParameter("causal"));
        if (!IdUtils.isValidBase64(fromId)) return ApiResult.error(Errors.fromMalformedParameter("fromId"));
        if (!IdUtils.isValidBase64(toId)) return ApiResult.error(Errors.fromMalformedParameter("toId"));
        if (fromId.equals(toId)) return ApiResult.error(Errors.fromMalformedParameter("toId"));
        if (amount <= 0) return ApiResult.error(Errors.fromMalformedParameter("amount"));
        if (causal.length() < 1 || causal.length() > Transfer.CAUSAL_LENGTH)
            return ApiResult.error(Errors.fromMalformedParameter("causal"));

        try {
            boolean prevAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                ApiResult<Tuple<Transfer, Tuple<Account, Account>>> objs = getToAndFrom(toId, fromId)
                        .flatMap(t -> checkToBalance(t, amount))
                        .flatMap(this::checkNotSame)
                        .flatMap(t -> createTransfer(t, amount, causal));
                if (objs.match(__ -> true, __ -> false)) {
                    return commitChanges(objs.get(), prevAutoCommit);
                } else {
                    if (prevAutoCommit) connection.rollback();
                    return ApiResult.error(objs.getError());
                }
            } catch (SQLException e) {
                if (prevAutoCommit) connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(prevAutoCommit);
            }
        } catch (SQLException e) {
            return ApiResult.error(Errors.fromSQLException(e));
        }
    }

    /**
     * Gets the origin and destination accounts from database
     */
    private ApiResult<Tuple<Account, Account>> getToAndFrom(String toId, String fromId) {
        ApiResult<Account> toResult = accountDAO.byId(toId);
        ApiResult<Account> fromResult = accountDAO.byId(fromId);
        return toResult.match(to ->
                        fromResult.match(from ->
                                        ApiResult.ok(new Tuple<>(to, from)),
                                ApiResult::error),
                ApiResult::error);
    }

    /**
     * Checks that the two accounts have different IDs
     */
    private ApiResult<Tuple<Account, Account>> checkNotSame(Tuple<Account, Account> accounts) {
        String first = accounts.getFirst().getBase64Id(),
                second = accounts.getSecond().getBase64Id();
        if (Objects.equals(first, second))
            return ApiResult.error(Errors.fromMalformedParameter("toId"));
        return ApiResult.ok(accounts);
    }

    /**
     * Checks that the origin account's balance is greater than the amount of money to transfer
     */
    private ApiResult<Tuple<Account, Account>> checkToBalance(Tuple<Account, Account> toAndFrom, double amount) {
        if (toAndFrom.getSecond().getBalance() < amount)
            return ApiResult.error(Errors.fromConflict("amount"));
        return ApiResult.ok(toAndFrom);
    }

    /**
     * Creates the new transfer bean and updates the accounts
     */
    private ApiResult<Tuple<Transfer, Tuple<Account, Account>>> createTransfer(Tuple<Account, Account> toAndFrom, double amount, String causal) {
        Account to = toAndFrom.getFirst();
        Account from = toAndFrom.getSecond();
        Transfer transfer = new Transfer();
        transfer.setFromId(from.getBase64Id());
        transfer.setToId(to.getBase64Id());
        transfer.setFromBalance(from.getBalance());
        transfer.setToBalance(to.getBalance());
        transfer.setAmount(amount);
        transfer.setCausal(causal);
        transfer.setDate(Instant.now());
        to.setBalance(to.getBalance() + amount);
        from.setBalance(from.getBalance() - amount);
        return ApiResult.ok(new Tuple<>(transfer, new Tuple<>(to, from)));
    }

    /**
     * Commits the objects to database, throws if something goes wrong
     */
    private ApiResult<Transfer> commitChanges(Tuple<Transfer, Tuple<Account, Account>> transferToAndFrom, boolean prevAutoCommit) throws SQLException {
        Transfer transfer = transferToAndFrom.getFirst();
        Account to = transferToAndFrom.getSecond().getFirst();
        Account from = transferToAndFrom.getSecond().getSecond();
        return accountDAO.update(to)
                .then(() -> accountDAO.update(from))
                .then(() -> insert(transfer))
                .match(t -> Result.of(() -> {
                            if (prevAutoCommit) connection.commit();
                            return ApiResult.ok(t);
                        }),
                        (Function<ApiError, Result<SQLException, ApiResult<Transfer>>>) e -> Result.of(() -> {
                            if (prevAutoCommit) connection.rollback();
                            return ApiResult.error(e);
                        }))
                .get();
    }

    /**
     * Inserts this {@link Transfer} into the database. If the object is already present, it returns an error, otherwise
     * the {@link Transfer} inserted.
     * <p>
     * The operation will be done atomically using transactions. If automatic transaction management has been turned
     * off, e.g. with {@link Connection#setAutoCommit(boolean)}, it is the caller's responsibility to commit
     * or rollback the changes.
     * <p>
     * Note: inserting {@link Transfer} objects directly is highly discouraged and could break data consistency since
     * nor the receiving nor the transmitting {@link Account}s will be updated. If you intend to create a new
     * {@link Transfer}, use {@link #newTransfer(String, String, double, String)}.
     *
     * @param transfer the {@link Transfer} to insert
     * @return an {@link ApiResult} containing an error or the saved object
     */
    @Override
    public ApiResult<Transfer> insert(Transfer transfer) {
        if (transfer == null) return ApiResult.error(Errors.fromNullParameter("transfer"));
        if (transfer.hasNullProperties(false)) return ApiResult.error(Errors.fromMalformedParameter("transfer"));
        if (!IdUtils.isValidBase64(transfer.getFromId()))
            return ApiResult.error(Errors.fromMalformedParameter("transfer.fromId"));
        if (!IdUtils.isValidBase64(transfer.getToId()))
            return ApiResult.error(Errors.fromMalformedParameter("transfer.toId"));
        if (transfer.getAmount() <= 0) return ApiResult.error(Errors.fromMalformedParameter("transfer.amount"));
        if (isPersisted(transfer)) return ApiResult.error(Errors.fromConflict("transfer"));

        try {
            String sql = "insert into tiw_app.transfers(id, date, amount, toId, toBalance, fromId, fromBalance, causal) values(?, ?, ?, ?, ?, ?, ?, ?)";
            boolean prevAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                long id = DAOUtils.genNewId(connection, "tiw_app.transfers", "id");
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setLong(1, id);
                    statement.setTimestamp(2, Timestamp.from(transfer.getDate()));
                    statement.setDouble(3, transfer.getAmount());
                    statement.setLong(4, IdUtils.fromBase64(transfer.getToId()));
                    statement.setDouble(5, transfer.getToBalance());
                    statement.setLong(6, IdUtils.fromBase64(transfer.getFromId()));
                    statement.setDouble(7, transfer.getFromBalance());
                    statement.setString(8, transfer.getCausal());
                    statement.executeUpdate();
                }
                if (prevAutoCommit) connection.commit();
                transfer.setBase64Id(IdUtils.toBase64(id));
                return ApiResult.ok(transfer);
            } catch (SQLException e) {
                if (prevAutoCommit) connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(prevAutoCommit);
            }
        } catch (SQLException e) {
            return ApiResult.error(Errors.fromSQLException(e));
        }
    }

    /**
     * Always returns an error since a {@link Transfer} cannot be updated.
     *
     * @param transfer the {@link Transfer} to update
     * @return an {@link ApiResult} containing an error
     */
    @Override
    public ApiResult<Transfer> update(Transfer transfer) {
        return ApiResult.error(new ApiError(400,
                "Operation not permitted",
                new ApiSubError("UnsupportedOperationException", "The operation requested is not allowed")));
    }

    /**
     * Checks whether the given {@link Transfer} is stored in the database or not
     *
     * @param transfer the object to check
     * @return true is the given object has a correspondent in the database
     */
    @Override
    public boolean isPersisted(Transfer transfer) {
        if (isNull(transfer)) return false;
        return byId(transfer.getBase64Id()).match(__ -> true, __ -> false);
    }

    /**
     * Returns a new TransferDAO object that will create all the DAO objects it
     * needs from scratch sharing the given {@link Connection}.
     *
     * @param connection a {@link Connection}
     * @return a new TransferDAO object
     * @throws NullPointerException if {@code connection} is null
     */
    public static TransferDAO withNewObjects(Connection connection) {
        requireNonNull(connection);
        return new TransferDAO(connection, new AccountDAO(connection));
    }
}
