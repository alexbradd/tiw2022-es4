package it.polimi.tiw.api;

import it.polimi.tiw.api.beans.LoginRequest;
import it.polimi.tiw.api.beans.RegistrationRequest;
import it.polimi.tiw.api.beans.User;
import it.polimi.tiw.api.dbaccess.ConnectionRetriever;
import it.polimi.tiw.api.dbaccess.UserDAO;
import it.polimi.tiw.api.error.ApiError;
import it.polimi.tiw.api.error.ApiSubError;
import it.polimi.tiw.api.error.Errors;
import it.polimi.tiw.api.functional.ApiResult;
import it.polimi.tiw.api.functional.Tuple;
import it.polimi.tiw.api.utils.PasswordUtils;

import java.sql.Connection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Class exposing a simple interface for manipulating users.
 */
public class UserFacade {
    private final Connection connection;
    private final Function<Connection, UserDAO> userDAOGenerator;
    private final AccountFacade accountFacade;

    /**
     * Creates a new UserFacade with the specified objects.
     *
     * @param connection       the {@link ConnectionRetriever} to use
     * @param userDAOGenerator a {@link Supplier} of {@link UserDAO}
     * @throws NullPointerException if any parameter is null
     */
    public UserFacade(Connection connection, Function<Connection, UserDAO> userDAOGenerator, AccountFacade accountFacade) {
        this.connection = Objects.requireNonNull(connection);
        this.userDAOGenerator = Objects.requireNonNull(userDAOGenerator);
        this.accountFacade = Objects.requireNonNull(accountFacade);
    }

    /**
     * Returns an {@link ApiResult} containing the {@link User} with the given username. An {@link ApiError} with code
     * 404 is returned if no such user can be found.
     *
     * @param username the username to search
     */
    public ApiResult<User> byUsername(String username) {
        return userDAOGenerator.apply(connection).byUsername(username);
    }

    /**
     * Handles a user registration request encoded by a {@link RegistrationRequest}. All fields are required to be
     * non-null and have a valid value. In case of success, an {@link ApiResult} containing the {@link User}
     * corresponding to the saved user. Otherwise, the returned value will contain an {@link ApiError} with the relative
     * information.
     * <p>
     * A new empty Account will be created for each newly registered user.
     *
     * @param req the {@link RegistrationRequest} to process
     * @return An {@link ApiResult} containing the User in case of success
     */
    public ApiResult<User> register(RegistrationRequest req) {
        if (req == null) return ApiResult.error(Errors.fromNullParameter("req"));
        return new User.Builder()
                .addUsername(req.getUsername())
                .addPassword(req.getClearPassword(), req.getRepeatPassword())
                .addEmail(req.getEmail())
                .addName(req.getName())
                .addSurname(req.getSurname())
                .build()
                .flatMap(u -> userDAOGenerator.apply(connection)
                        .insert(u)
                        .then(() -> accountFacade.createFor(u.getBase64Id()))
                        .then(() -> ApiResult.ok(u)));
    }

    /**
     * Handles a login request encoded by a {@link LoginRequest}. If the user can be authenticated, that {@link User} is
     * returned. Otherwise, a suitable error is returned.
     *
     * @param req The {@link LoginRequest} to analyze
     * @return An {@link ApiResult} containing the User in case of success
     */
    public ApiResult<User> authorize(LoginRequest req) {
        if (req == null) return ApiResult.error(Errors.fromNullParameter("req"));
        Tuple<String, String> username = new Tuple<>(req.getUsername(), "username");
        Tuple<String, String> clearPassword = new Tuple<>(req.getClearPassword(), "clearPassword");

        List<ApiSubError> e = Stream.of(username, clearPassword)
                .filter(t -> t.getFirst() == null)
                .map(t -> new ApiSubError("NoSuchElementException", t.getSecond() + "is a required parameter"))
                .toList();
        if (e.size() > 0)
            return ApiResult.error(new ApiError(400, "Missing required parameter", e.toArray(new ApiSubError[0])));
        return userDAOGenerator.apply(connection)
                .byUsername(username.getFirst())
                .flatMap(u -> {
                    if (PasswordUtils.match(u.getSaltedPassword(), clearPassword.getFirst()))
                        return ApiResult.ok(u);
                    return ApiResult.error(new ApiError(
                            409,
                            "Username doesn't match password",
                            new ApiSubError("IllegalArgumentException",
                                    "The given password doesn't match the one saved")
                    ));
                });
    }

    /**
     * Creates a new UserFacade using the default objects
     *
     * @param connection the {@link Connection} to use
     * @return a new UserFacade
     */
    public static UserFacade withDefaultObjects(Connection connection) {
        return new UserFacade(connection, UserDAO::new, AccountFacade.withDefaultObjects(connection));
    }
}
