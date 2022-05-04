package it.polimi.tiw.api.dbaccess;

import it.polimi.tiw.api.ApiError;
import it.polimi.tiw.api.ApiResult;
import it.polimi.tiw.api.ApiSubError;
import it.polimi.tiw.api.beans.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

/**
 * Class for retrieving {@link User} instances from a database.
 */
public class UserDAO implements DatabaseAccessObject<User> {
    /**
     * The ConnectionRetriever object used to get new connections to the database
     */
    private final ConnectionRetriever retriever;

    /**
     * Instantiates a new UserDAO using the default {@link ConnectionRetriever}.
     */
    public UserDAO() {
        this.retriever = ProductionConnectionRetriever.getInstance();
    }

    /**
     * Instantiates a new UserDAO using the given {@link ConnectionRetriever}.
     *
     * @param retriever the {@link ConnectionRetriever} to use.
     * @throws NullPointerException if {@code retriever} is null
     */
    public UserDAO(ConnectionRetriever retriever) {
        Objects.requireNonNull(retriever);
        this.retriever = retriever;
    }

    /**
     * Injects strings into PreparedStatement
     */
    private static void injectStringParameters(PreparedStatement p, String... vars) throws SQLException {
        for (int i = 0; i < vars.length; i++)
            p.setString(i + 1, vars[i]);
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
        try (Connection c = retriever.getConnection()) {
            return byUsername(c, username);
        } catch (SQLException e) {
            ApiError error = new ApiError(500,
                    "Error while fetching data",
                    new ApiSubError("SQLException", e.getMessage() == null ? "" : e.getMessage()));
            return ApiResult.error(error);
        }
    }

    /**
     * Like {@link #byUsername(String)}, however use the given Connection instead of retrieving one from the
     * ConnectionRetrieve
     *
     * @param c        the Connection to e
     * @param username the username to search
     * @return an {@link ApiResult} containing the constructed User
     * @throws SQLException if any database related error occurred
     */
    public ApiResult<User> byUsername(Connection c, String username) throws SQLException {
        Objects.requireNonNull(c);
        Objects.requireNonNull(username);
        try (PreparedStatement p = c.prepareStatement("select * from tiw_app.users where username = ?")) {
            injectStringParameters(p, username);
            return packageApiResult(p, username);
        }
    }

    /**
     * Executes p and wraps the result in an ApiResult
     */
    private ApiResult<User> packageApiResult(PreparedStatement p, String specifier) throws SQLException {
        try (ResultSet r = p.executeQuery()) {
            if (r.next()) {
                return new User.Builder()
                        .addUsername(r.getString("username"))
                        .addPassword(r.getString("password"))
                        .addEmail(r.getString("email"))
                        .addName(r.getString("name"))
                        .addSurname(r.getString("surname"))
                        .build();
            } else {
                ApiError error = new ApiError(404,
                        "Cannot find this object",
                        new ApiSubError("NoSuchElementException", "No user with " + specifier));
                return ApiResult.error(error);
            }
        }
    }

    /**
     * Saves the current User to database. If the User already existed, the updates are stored.
     *
     * @param user the User to save
     * @return an {@link ApiResult} containing the User just saved.
     * @throws NullPointerException of {@code u} is null or any property of {@code u} is null
     */
    @Override
    public ApiResult<User> save(User user) {
        Objects.requireNonNull(user);
        Objects.requireNonNull(user.getUsername());
        Objects.requireNonNull(user.getSaltedPassword());
        Objects.requireNonNull(user.getEmail());
        Objects.requireNonNull(user.getName());
        Objects.requireNonNull(user.getSurname());
        try (Connection c = retriever.getConnection()) {
            c.setAutoCommit(false);
            try {
                if (byUsername(c, user.getUsername()).match((User u) -> true, (ApiError e) -> false))
                    updateUser(c, user);
                else
                    saveNewUser(c, user);
                c.commit();
                return ApiResult.ok(user);
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException e) {
            ApiError error = new ApiError(409,
                    "Cannot save this object",
                    new ApiSubError("SQLException", e.getMessage() == null ? "" : e.getMessage()));
            return ApiResult.error(error);
        }
    }

    /**
     * Checks whether a User has already been saved to the database
     */
    private boolean isPersisted(Connection c, User u) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("select * from tiw_app.users where username = ?")) {
            p.setString(1, u.getUsername());
            try (ResultSet r = p.executeQuery()) {
                return r.next();
            }
        }
    }

    /**
     * Updates the already existing user represented by this object
     */
    private void updateUser(Connection c, User u) throws SQLException {
        try (PreparedStatement p = c.prepareStatement(
                "update tiw_app.users set password = ?, email = ?, name = ?, surname = ? where username = ?")) {
            injectStringParameters(p, u.getSaltedPassword(), u.getEmail(), u.getName(), u.getSurname(), u.getUsername());
            p.executeUpdate();
        }
    }

    /**
     * Save a new user with his object's properties into the database
     */
    private void saveNewUser(Connection c, User u) throws SQLException {
        try (PreparedStatement p = c.prepareStatement(
                "insert into tiw_app.users(username, password, email, name, surname) values (?, ?, ?, ?, ?)")) {
            injectStringParameters(p, u.getUsername(), u.getSaltedPassword(), u.getEmail(), u.getName(), u.getSurname());
            p.executeUpdate();
        }
    }
}
