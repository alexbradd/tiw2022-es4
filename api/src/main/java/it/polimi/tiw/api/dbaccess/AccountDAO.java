package it.polimi.tiw.api.dbaccess;

import it.polimi.tiw.api.ApiResult;
import it.polimi.tiw.api.beans.Account;
import it.polimi.tiw.api.beans.User;
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
    private final UserDAO userDAO;

    /**
     * Instantiates a new AccountDAO using the given {@link Connection} and {@link UserDAO}.
     *
     * @param connection the {@link Connection} to use.
     * @param userDAO    the {@link UserDAO} to user.
     * @throws NullPointerException if any parameter is null
     */
    public AccountDAO(Connection connection, UserDAO userDAO) {
        requireNonNull(connection);
        requireNonNull(userDAO);
        this.connection = connection;
        this.userDAO = userDAO;
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
                    return userDAO
                            .byId(ownerId)
                            .map(u -> {
                                Account a = new Account(u, balance);
                                a.setBase64Id(IdUtils.toBase64(id));
                                return a;
                            });
                } else
                    return ApiResult.error(DAOUtils.fromMissingElement("id " + IdUtils.toBase64(id)));
            }
        } catch (SQLException e) {
            return ApiResult.error(DAOUtils.fromSQLException(e));
        }
    }

    /**
     * Returns an ApiResult containing all the Accounts associated with the given {@link User}
     *
     * @param owner the User that is the owner of the accounts
     * @return an ApiResult containing all the Accounts associated with the given {@link User}
     */
    public ApiResult<List<Account>> ofUser(User owner) {
        if (isNull(owner)) return ApiResult.error(DAOUtils.fromNullParameter("owner"));
        if (isNull(owner.getBase64Id())) return ApiResult.error(DAOUtils.fromMalformedParameter("owner"));
        if (!IdUtils.isValidBase64(owner.getBase64Id()))
            return ApiResult.error(DAOUtils.fromMalformedParameter("owner"));
        long userId = IdUtils.fromBase64(owner.getBase64Id());
        String sql = "select * from tiw_app.accounts where ownerId = ?";
        ArrayList<Account> accs = new ArrayList<>();
        try (PreparedStatement p = connection.prepareStatement(sql)) {
            p.setLong(1, userId);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    String id = IdUtils.toBase64(r.getLong("id"));
                    int balance = r.getInt("balance");
                    Account a = new Account(owner, balance);
                    a.setBase64Id(id);
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
                    p.setLong(1, IdUtils.fromBase64(account.getOwner().getBase64Id()));
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
                    p.setLong(2, IdUtils.fromBase64(account.getOwner().getBase64Id()));
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

    /**
     * Returns a new AccountDAO object that will create all the DAO objects it
     * needs from scratch sharing the given {@link Connection}.
     *
     * @param connection a {@link Connection}
     * @return a new AccountDAO object
     * @throws NullPointerException if {@code connection} is null
     */
    public static AccountDAO withNewObjects(Connection connection) {
        requireNonNull(connection);
        return new AccountDAO(connection, new UserDAO(connection));
    }
}
