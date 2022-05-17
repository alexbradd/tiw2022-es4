package it.polimi.tiw.api;

import it.polimi.tiw.api.beans.User;
import it.polimi.tiw.api.dbaccess.ConnectionRetriever;
import it.polimi.tiw.api.dbaccess.UserDAO;
import it.polimi.tiw.api.error.ApiError;
import it.polimi.tiw.api.error.ApiSubError;
import it.polimi.tiw.api.functional.ApiResult;
import it.polimi.tiw.api.functional.Tuple;
import it.polimi.tiw.api.utils.PasswordUtils;

import javax.servlet.http.HttpServletRequest;
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
     * Handles user registration. The request must provide the following fields (as either query parameters or form
     * data):
     *
     * <ul>
     *     <li>Field 'username': the username of the new user</li>
     *     <li>Field 'clearPassword': the cleartext password of the new user</li>
     *     <li>Field 'repeatPassword': a repetition of the cleartext password of the new user</li>
     *     <li>Field 'email':  the email of the new user</li>
     *     <li>Field 'name':  the name of the new user</li>
     *     <li>Field 'surname':  the surname of the new user</li>
     * </ul>
     * <p>
     * All fields are mandatory, must be urlencoded and have a maximum length of 128 characters. If data is passed in
     * both ways (querystring and form data) only one of the two sources will be used. In case of success, an
     * {@link ApiResult} containing the {@link UserDAO} corresponding to the saved user. Otherwise, the returned value will
     * contain an {@link ApiError} with the relative information.
     *
     * A new empty Account will be created for the newly registered user.
     *
     * @param req the {@link HttpServletRequest} to process
     * @return An {@link ApiResult} containing the User in case of success
     * @throws NullPointerException if {@code req} is null
     */
    public ApiResult<User> register(HttpServletRequest req) {
        Objects.requireNonNull(req);
        return new User.Builder()
                .addUsername(req.getParameter("username"))
                .addPassword(req.getParameter("clearPassword"), req.getParameter("repeatPassword"))
                .addEmail(req.getParameter("email"))
                .addName(req.getParameter("name"))
                .addSurname(req.getParameter("surname"))
                .build()
                .flatMap(u -> userDAOGenerator.apply(connection)
                        .insert(u)
                        .then(() -> accountFacade.createFor(u.getBase64Id()))
                        .then(() -> ApiResult.ok(u)));
    }

    /**
     * Authenticates a User with the username and password specified in the given request. If the user can be
     * authenticated, that User is returned. Otherwise, a suitable error is returned. The parameters required are:
     *
     * <ol>
     *     <li>'username': the User's username</li>
     *     <li>'clearPassword': the User's clear text password</li>
     * </ol>
     *
     * @param req The {@link HttpServletRequest} to analyze
     * @return An {@link ApiResult} containing the User in case of success
     * @throws NullPointerException if {@code req} is null
     */
    public ApiResult<User> authorize(HttpServletRequest req) {
        Objects.requireNonNull(req);
        Tuple<String, String> username = new Tuple<>(req.getParameter("username"), "username");
        Tuple<String, String> clearPassword = new Tuple<>(req.getParameter("clearPassword"), "clearPassword");

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
