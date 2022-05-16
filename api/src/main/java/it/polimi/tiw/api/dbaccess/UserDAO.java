package it.polimi.tiw.api.dbaccess;

import it.polimi.tiw.api.ApiError;
import it.polimi.tiw.api.ApiResult;
import it.polimi.tiw.api.beans.User;
import it.polimi.tiw.api.utils.IdUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

/**
 * Class for retrieving {@link User} instances from a database.
 */
public class UserDAO implements DatabaseAccessObject<User> {

    private final Connection connection;

    /**
     * Instantiates a new UserDAO using the given {@link Connection}.
     *
     * @param connection the {@link Connection} to use.
     * @throws NullPointerException if {@code connection} is null
     */
    public UserDAO(Connection connection) {
        requireNonNull(connection);
        this.connection = connection;
    }

    /**
     * Injects strings into PreparedStatement
     */
    private static void injectStringParameters(PreparedStatement p, String... vars) throws SQLException {
        for (int i = 0; i < vars.length; i++)
            p.setString(i + 1, vars[i]);
    }

    /**
     * Finds and retrieves the data for the User with the given id. If no such user can be found, an empty
     * {@link ApiResult} is returned.
     *
     * @param id the id to search
     * @return an {@link ApiResult} containing the constructed User
     */
    ApiResult<User> byId(long id) {
        String sql = "select * from tiw_app.users where id = ?";
        try (PreparedStatement p = connection.prepareStatement(sql)) {
            p.setLong(1, id);
            return packageApiResult(p, String.valueOf(id));
        } catch (SQLException e) {
            return ApiResult.error(DAOUtils.fromSQLException(e));
        }
    }

    /**
     * Finds and retrieves the data for the User with the given id. If no such user can be found, an empty
     * {@link ApiResult} is returned.
     *
     * @param base64Id the id to search
     * @return an {@link ApiResult} containing the constructed User
     */
    @Override
    public ApiResult<User> byId(String base64Id) {
        if (isNull(base64Id)) return ApiResult.error(DAOUtils.fromNullParameter("base64Id"));
        if (!IdUtils.isValidBase64(base64Id)) return ApiResult.error(DAOUtils.fromMalformedParameter("base64Id"));
        return byId(IdUtils.fromBase64(base64Id));
    }

    /**
     * Finds and retrieves the data for the User with the given username. If no such user can be found, an empty
     * {@link ApiResult} is returned.
     *
     * @param username the username to search
     * @return an {@link ApiResult} containing the constructed User
     */
    public ApiResult<User> byUsername(String username) {
        if (isNull(username)) return ApiResult.error(DAOUtils.fromNullParameter("username"));
        String sql = "select * from tiw_app.users where username = ?";
        try (PreparedStatement p = connection.prepareStatement(sql)) {
            injectStringParameters(p, username);
            return packageApiResult(p, username);
        } catch (SQLException e) {
            return ApiResult.error(DAOUtils.fromSQLException(e));
        }
    }

    /**
     * Executes p and wraps the result in an ApiResult
     */
    private ApiResult<User> packageApiResult(PreparedStatement p, String specifier) throws SQLException {
        try (ResultSet r = p.executeQuery()) {
            if (r.next()) {
                return new User.Builder()
                        .addId(IdUtils.toBase64(r.getLong("id")))
                        .addUsername(r.getString("username"))
                        .addPassword(r.getString("password"))
                        .addEmail(r.getString("email"))
                        .addName(r.getString("name"))
                        .addSurname(r.getString("surname"))
                        .build();
            } else {
                return ApiResult.error(DAOUtils.fromMissingElement(specifier));
            }
        }
    }

    /**
     * Checks whether the given User is stored in the database or not
     *
     * @param user the User to check
     * @return true is the given User has a correspondent in the database
     */
    @Override
    public boolean isPersisted(User user) {
        if (isNull(user)) return false;
        return byId(user.getBase64Id()).match(u -> true, (ApiError e) -> false);
    }

    /**
     * Updates the entity corresponding to this User in the database. If the User is not present in the database,
     * an error is returned. Otherwise, the User passed is returned.
     * <p>
     * The operation will be done atomically using transactions. If automatic transaction management has been turned
     * off, e.g. with {@link java.sql.Connection#setAutoCommit(boolean)}, it is the caller's responsibility to commit
     * or rollback the changes.
     *
     * @param user the User to update
     * @return an {@link ApiResult} containing an error or the updated object
     */
    @Override
    public ApiResult<User> update(User user) {
        if (isNull(user)) return ApiResult.error(DAOUtils.fromNullParameter("user"));
        if (user.hasNullProperties(true)) return ApiResult.error(DAOUtils.fromMalformedParameter("user"));
        if (!IdUtils.isValidBase64(user.getBase64Id()))
            return ApiResult.error(DAOUtils.fromMalformedParameter("user"));
        if (!isPersisted(user)) return ApiResult.error(DAOUtils.fromMalformedParameter("user"));

        try {
            boolean prevAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement p = connection.prepareStatement(
                        "update tiw_app.users set username = ?, password = ?, email = ?, name = ?, surname = ? where id = ?")) {
                    injectStringParameters(p, user.getUsername(), user.getSaltedPassword(), user.getEmail(), user.getName(), user.getSurname());
                    p.setLong(6, IdUtils.fromBase64(user.getBase64Id()));
                    p.executeUpdate();
                }
                if (prevAutoCommit) connection.commit();
                return ApiResult.ok(user);
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
     * Inserts this User into the database. If the User is already present or another user with the same username exists,
     * an error is returned. Otherwise, the passed User with the assigned id is returned.
     * <p>
     * The operation will be done atomically using transactions. If automatic transaction management has been turned
     * off, e.g. with {@link java.sql.Connection#setAutoCommit(boolean)}, it is the caller's responsibility to commit
     * or rollback the changes.
     *
     * @param user the object to insert
     * @return an {@link ApiResult} containing an error or the inserted User
     */
    @Override
    public ApiResult<User> insert(User user) {
        if (isNull(user)) return ApiResult.error(DAOUtils.fromNullParameter("user"));
        if (user.hasNullProperties(false)) return ApiResult.error(DAOUtils.fromMalformedParameter("user"));
        if (isPersisted(user)) return ApiResult.error(DAOUtils.fromMalformedParameter("user"));
        if (byUsername(user.getUsername()).match(__ -> true, __ -> false))
            return ApiResult.error(DAOUtils.fromConflict("user"));

        try {
            boolean prevAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                long id = DAOUtils.genNewId(connection, "tiw_app.users", "id");
                try (PreparedStatement p = connection.prepareStatement(
                        "insert into tiw_app.users(username, password, email, name, surname, id) values (?, ?, ?, ?, ?, ?)")) {
                    injectStringParameters(p, user.getUsername(), user.getSaltedPassword(), user.getEmail(), user.getName(), user.getSurname());
                    p.setLong(6, id);
                    p.executeUpdate();
                }
                if (prevAutoCommit) connection.commit();
                user.setBase64Id(IdUtils.toBase64(id));
                return ApiResult.ok(user);
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
