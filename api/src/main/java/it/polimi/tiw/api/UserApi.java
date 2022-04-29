package it.polimi.tiw.api;

import it.polimi.tiw.api.beans.User;
import it.polimi.tiw.api.dbaccess.UserDAO;
import it.polimi.tiw.api.functional.Tuple;
import it.polimi.tiw.api.utils.PasswordUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Container for all {@link UserDAO} related calls
 */
public class UserApi {

    /**
     * Class is static
     */
    private UserApi() {
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
     * contain an {@link ApiError} with the relative information
     * @param req the {@link HttpServletRequest} to process
     * @return An {@link ApiResult} containing the User in case of success
     * @throws NullPointerException if {@code req} is null
     */
    public static ApiResult<User> register(HttpServletRequest req) {
        Objects.requireNonNull(req);
        return new User.Builder()
                .addUsername(req.getParameter("username"))
                .addPassword(req.getParameter("clearPassword"), req.getParameter("repeatPassword"))
                .addEmail(req.getParameter("email"))
                .addName(req.getParameter("name"))
                .addSurname(req.getParameter("username"))
                .build()
                .flatMap(u -> new UserDAO().save(u));
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
    public static ApiResult<User> authorize(HttpServletRequest req) {
        Objects.requireNonNull(req);
        Tuple<String, String> username = new Tuple<>(req.getParameter("username"), "username");
        Tuple<String, String> clearPassword = new Tuple<>(req.getParameter("clearPassword"), "clearPassword");

        List<ApiSubError> e = Stream.of(username, clearPassword)
                .filter(t -> t.getFirst() == null)
                .map(t -> new ApiSubError("NoSuchElementException", t.getSecond() + "is a required parameter"))
                .toList();
        if (e.size() > 0)
            return ApiResult.error(new ApiError(400, "Missing required parameter", e.toArray(new ApiSubError[0])));
        return new UserDAO().byUsername(username.getFirst())
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
}
