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
import java.util.Objects;

/**
 * Class for retrieving {@link Account} instances from a database.
 */
public class AccountDAO implements DatabaseAccessObject<Account> {
    private final Connection connection;

    /**
     * Instantiates a new AccountDAO using the given {@link Connection}.
     *
     * @param connection the {@link Connection} to use.
     * @throws NullPointerException if {@code connection} is null
     */
    public AccountDAO(Connection connection) {
        Objects.requireNonNull(connection);
        this.connection = connection;
    }

    /**
     * Finds and retrieves the data for the Account with the given id. If no such account can be found, an empty
     * {@link ApiResult} is returned.
     *
     * @param base64Id the id to search
     * @return an {@link ApiResult} containing the constructed User
     * @throws NullPointerException     if {@code base64Id} is null
     * @throws IllegalArgumentException if {@code base64Id} is not a valid base64 encoded string (see {@link IdUtils})
     */
    public ApiResult<Account> byId(String base64Id) {
        Objects.requireNonNull(base64Id);
        long id = IdUtils.fromBase64(base64Id);
        try (PreparedStatement p = connection.prepareStatement("select * from tiw_app.accounts where id = ?")) {
            p.setLong(1, id);
            try (ResultSet r = p.executeQuery()) {
                if (r.next()) {
                    long ownerId = r.getLong("ownerId");
                    int balance = r.getInt("balance");
                    return new UserDAO(connection)
                            .byId(ownerId)
                            .map(u -> {
                                Account a = new Account(u, balance);
                                a.setBase64Id(base64Id);
                                return a;
                            });
                } else
                    return ApiResult.error(DAOUtils.fromMissingElement("id " + base64Id));
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
     * @throws NullPointerException     if {@code owner} is null or its id is
     * @throws IllegalArgumentException if {@code owner}'s id is not valid base64 (see {@link IdUtils})
     */
    public ApiResult<List<Account>> ofUser(User owner) {
        Objects.requireNonNull(owner);
        Objects.requireNonNull(owner.getBase64Id());
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
     * Saves this Account to the database. If the object is already present, it updates the existing one.
     *
     * @param account the Account to save
     * @return an {@link ApiResult} containing an error is something went wrong or the saved object is the operation was
     * a success
     * @throws NullPointerException     if {@code account} of {@code account.getOwner()} are null
     * @throws IllegalArgumentException if the balance of the account is negative
     * @throws IllegalArgumentException if {@code account.getBase64Id()} or {@code account.getOwner().getBase64Id()} do
     *                                  not return valid base 64 strings (see {@link IdUtils})
     */
    @Override
    public ApiResult<Account> save(Account account) {
        Objects.requireNonNull(account);
        Objects.requireNonNull(account.getOwner());
        if (account.getBalance() < 0)
            throw new IllegalArgumentException("Cannot save an account with negative balance");
        try {
            connection.setAutoCommit(false);
            try {
                if (isPersisted(account)) updateAccount(account);
                else addNewAccount(account);
                connection.commit();
                return ApiResult.ok(account);
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
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
     * @throws NullPointerException if {@code account} is null
     */
    @Override
    public boolean isPersisted(Account account) {
        Objects.requireNonNull(account);
        return account.getBase64Id() != null && byId(account.getBase64Id()).match(__ -> true, __ -> false);
    }

    /**
     * Updates the account with the given id to the owner and balance
     */
    private void updateAccount(Account account) throws SQLException {
        String sql = "update tiw_app.accounts set ownerId = ?, balance = ? where id = ?";
        try (PreparedStatement p = connection.prepareStatement(sql)) {
            p.setLong(1, IdUtils.fromBase64(account.getOwner().getBase64Id()));
            p.setInt(2, account.getBalance());
            p.setLong(3, IdUtils.fromBase64(account.getBase64Id()));
            p.executeUpdate();
        }
    }

    /**
     * Saves a new account to db and sets the new id
     */
    private void addNewAccount(Account account) throws SQLException {
        String sql = "insert into tiw_app.accounts(id, ownerId, balance) values(?, ?, ?);";
        long id = DAOUtils.genNewId(connection, "tiw_app.accounts", "id");
        try (PreparedStatement p = connection.prepareStatement(sql)) {
            p.setLong(1, id);
            p.setLong(2, IdUtils.fromBase64(account.getOwner().getBase64Id()));
            p.setInt(3, account.getBalance());
            p.executeUpdate();
            account.setBase64Id(IdUtils.toBase64(id));
        }
    }
}
