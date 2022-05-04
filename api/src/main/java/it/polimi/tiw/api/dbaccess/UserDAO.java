package it.polimi.tiw.api.dbaccess;

import it.polimi.tiw.api.ApiError;
import it.polimi.tiw.api.ApiResult;
import it.polimi.tiw.api.beans.User;
import it.polimi.tiw.api.utils.IdUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

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
        Objects.requireNonNull(connection);
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
     * Finds and retrieves the data for the User with the given username. If no such user can be found, an empty
     * {@link ApiResult} is returned.
     *
     * @param username the username to search
     * @return an {@link ApiResult} containing the constructed User
     * @throws NullPointerException if {@code username} is null
     */
    public ApiResult<User> byUsername(String username) {
        Objects.requireNonNull(username, "username is required");
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
     * Saves the current User to database. If the User already existed, the updates are stored.
     *
     * @param user the User to save
     * @return an {@link ApiResult} containing the User just saved.
     * @throws NullPointerException     if {@code user} is null or any property of {@code user} is null
     * @throws IllegalArgumentException if {@code user}'s id is not valid base64 (see {@link IdUtils})
     */
    @Override
    public ApiResult<User> save(User user) {
        Objects.requireNonNull(user);
        Objects.requireNonNull(user.getUsername());
        Objects.requireNonNull(user.getSaltedPassword());
        Objects.requireNonNull(user.getEmail());
        Objects.requireNonNull(user.getName());
        Objects.requireNonNull(user.getSurname());
        try {
            connection.setAutoCommit(false);
            try {
                if (isPersisted(user)) updateUser(user);
                else saveNewUser(user);
                connection.commit();
                return ApiResult.ok(user);
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
     * Checks whether the given User is stored in the database or not
     *
     * @param user the User to check
     * @return true is the given User has a correspondent in the database
     * @throws NullPointerException if {@code user} is null
     */
    @Override
    public boolean isPersisted(User user) {
        Objects.requireNonNull(user);
        return byUsername(user.getUsername()).match((User u) -> true, (ApiError e) -> false);
    }

    /**
     * Updates the already existing user represented by this object
     */
    private void updateUser(User u) throws SQLException {
        try (PreparedStatement p = connection.prepareStatement(
                "update tiw_app.users set username = ?, password = ?, email = ?, name = ?, surname = ? where id = ?")) {
            injectStringParameters(p, u.getUsername(), u.getSaltedPassword(), u.getEmail(), u.getName(), u.getSurname());
            p.setLong(6, IdUtils.fromBase64(u.getBase64Id()));
            p.executeUpdate();
        }
    }

    /**
     * Save a new user with his object's properties into the database
     */
    private void saveNewUser(User u) throws SQLException {
        long id = DAOUtils.genNewId(connection, "tiw_app.users", "id");
        try (PreparedStatement p = connection.prepareStatement(
                "insert into tiw_app.users(username, password, email, name, surname, id) values (?, ?, ?, ?, ?, ?)")) {
            injectStringParameters(p, u.getUsername(), u.getSaltedPassword(), u.getEmail(), u.getName(), u.getSurname());
            p.setLong(6, id);
            p.executeUpdate();
            u.setBase64Id(IdUtils.toBase64(id));
        }
    }
}
