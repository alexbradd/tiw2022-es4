package it.polimi.tiw.api.dbaccess;

import it.polimi.tiw.api.beans.Account;
import it.polimi.tiw.api.beans.User;
import it.polimi.tiw.api.functional.ApiResult;
import it.polimi.tiw.api.utils.IdUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

/**
 * Class for retrieving {@link Account} instances from a database.
 */
public class AccountDAO implements DatabaseAccessObject<Account> {
    private final Connection connection;

    /**
     * Instantiates a new AccountDAO using the given {@link Connection}
     *
     * @param connection the {@link Connection} to use.
     * @throws NullPointerException if any parameter is null
     */
    public AccountDAO(Connection connection) {
        requireNonNull(connection);
        this.connection = connection;
    }

    /**
     * Finds and retrieves the data for the Account with the given id. If no such account can be found, an empty
     * {@link ApiResult} is returned.
     *
     * @param base64Id the id to search
     * @return an {@link ApiResult} containing the constructed Account
     */
    @Override
    public ApiResult<Account> byId(String base64Id) {
        if (isNull(base64Id)) return ApiResult.error(DAOUtils.fromNullParameter("base64Id"));
        if (!IdUtils.isValidBase64(base64Id)) return ApiResult.error(DAOUtils.fromMalformedParameter("base64Id"));
        long id = IdUtils.fromBase64(base64Id);
        return byId(id);
    }

    /**
     * Finds and retrieves the data for the Account with the given id. If no such account can be found, an empty
     * {@link ApiResult} is returned.
     *
     * @param id the id to search
     * @return an {@link ApiResult} containing the constructed Account
     */
    ApiResult<Account> byId(long id) {
        try (PreparedStatement p = connection.prepareStatement("select * from tiw_app.accounts where id = ?")) {
            p.setLong(1, id);
            try (ResultSet r = p.executeQuery()) {
                if (r.next()) {
                    long ownerId = r.getLong("ownerId");
                    int balance = r.getInt("balance");
                    Account a = new Account(IdUtils.toBase64(id), IdUtils.toBase64(ownerId), balance);
                    return ApiResult.ok(a);
                } else
                    return ApiResult.error(DAOUtils.fromMissingElement("id " + IdUtils.toBase64(id)));
            }
        } catch (SQLException e) {
            return ApiResult.error(DAOUtils.fromSQLException(e));
        }
    }

    /**
     * Returns an ApiResult containing all the Accounts associated with the {@link User} with the given id
     *
     * @param ownerId the User that is the owner of the accounts
     * @return an ApiResult containing all the Accounts associated with the given {@link User}
     */
    public ApiResult<List<Account>> ofUser(String ownerId) {
        if (isNull(ownerId)) return ApiResult.error(DAOUtils.fromNullParameter("owner"));
        if (!IdUtils.isValidBase64(ownerId))
            return ApiResult.error(DAOUtils.fromMalformedParameter("owner"));
        long userId = IdUtils.fromBase64(ownerId);
        String sql = "select * from tiw_app.accounts where ownerId = ?";
        ArrayList<Account> accs = new ArrayList<>();
        try (PreparedStatement p = connection.prepareStatement(sql)) {
            p.setLong(1, userId);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    String id = IdUtils.toBase64(r.getLong("id"));
                    int balance = r.getInt("balance");
                    Account a = new Account(id, ownerId, balance);
                    accs.add(a);
                }
                return ApiResult.ok(accs);
            }
        } catch (SQLException e) {
            return ApiResult.error(DAOUtils.fromSQLException(e));
        }
    }

    /**
     * Checks whether the given Account is stored in the database or not
     *
     * @param account the Account to check
     * @return true is the given Account has a correspondent in the database
     */
    @Override
    public boolean isPersisted(Account account) {
        if (isNull(account)) return false;
        return byId(account.getBase64Id()).match(__ -> true, __ -> false);
    }

    /**
     * Updates the entity corresponding to this Account in the database. If the Account is not present in the database,
     * an error is returned. Otherwise, the object passed is returned.
     * <p>
     * The operation will be done atomically using transactions. If automatic transaction management has been turned
     * off, e.g. with {@link java.sql.Connection#setAutoCommit(boolean)}, it is the caller's responsibility to commit
     * or rollback the changes.
     *
     * @param account the Account to update
     * @return an {@link ApiResult} containing an error or the updated object
     */
    @Override
    public ApiResult<Account> update(Account account) {
        if (isNull(account)) return ApiResult.error(DAOUtils.fromNullParameter("account"));
        if (account.hasNullProperties(true)) return ApiResult.error(DAOUtils.fromMalformedParameter("account"));
        if (account.getBalance() < 0)
            return ApiResult.error(DAOUtils.fromMalformedParameter("account"));
        if (!IdUtils.isValidBase64(account.getBase64Id()))
            return ApiResult.error(DAOUtils.fromMalformedParameter("account"));
        if (!isPersisted(account)) return ApiResult.error(DAOUtils.fromMalformedParameter("account"));

        try {
            String sql = "update tiw_app.accounts set ownerId = ?, balance = ? where id = ?";
            boolean prevAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement p = connection.prepareStatement(sql)) {
                    p.setLong(1, IdUtils.fromBase64(account.getOwnerId()));
                    p.setInt(2, account.getBalance());
                    p.setLong(3, IdUtils.fromBase64(account.getBase64Id()));
                    p.executeUpdate();
                }
                if (prevAutoCommit) connection.commit();
                return ApiResult.ok(account);
            } catch (SQLException e) {
                if (prevAutoCommit) connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(prevAutoCommit);
            }
        } catch (SQLException e) {
            return ApiResult.error(DAOUtils.fromSQLException(e));
        }
    }

    /**
     * Inserts this object into the database. If the object is already present, it returns an error, otherwise the object
     * inserted.
     * <p>
     * The operation will be done atomically using transactions. If automatic transaction management has been turned
     * off, e.g. with {@link java.sql.Connection#setAutoCommit(boolean)}, it is the caller's responsibility to commit
     * or rollback the changes.
     *
     * @param account the object to insert
     * @return an {@link ApiResult} containing an error or the saved object
     */
    @Override
    public ApiResult<Account> insert(Account account) {
        if (isNull(account)) return ApiResult.error(DAOUtils.fromNullParameter("account"));
        if (account.hasNullProperties(false)) return ApiResult.error(DAOUtils.fromMalformedParameter("account"));
        if (account.getBalance() < 0)
            return ApiResult.error(DAOUtils.fromMalformedParameter("account"));
        if (isPersisted(account)) return ApiResult.error(DAOUtils.fromMalformedParameter("account"));

        try {
            String sql = "insert into tiw_app.accounts(id, ownerId, balance) values(?, ?, ?);";
            boolean prevAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                long id = DAOUtils.genNewId(connection, "tiw_app.accounts", "id");
                try (PreparedStatement p = connection.prepareStatement(sql)) {
                    p.setLong(1, id);
                    p.setLong(2, IdUtils.fromBase64(account.getOwnerId()));
                    p.setInt(3, account.getBalance());
                    p.executeUpdate();
                }
                if (prevAutoCommit) connection.commit();
                account.setBase64Id(IdUtils.toBase64(id));
                return ApiResult.ok(account);
            } catch (SQLException e) {
                if (prevAutoCommit) connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(prevAutoCommit);
            }
        } catch (SQLException e) {
            return ApiResult.error(DAOUtils.fromSQLException(e));
        }
    }
}
