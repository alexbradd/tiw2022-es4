package it.polimi.tiw.api;

import it.polimi.tiw.api.dbaccess.User;
import it.polimi.tiw.api.dbaccess.UserBuilder;

import javax.servlet.http.HttpServletRequest;

/**
 * Container for all {@link User} related calls
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
     * {@link ApiResult} containing the {@link User} corresponding to the saved user. Otherwise, the returned value will
     * contain an {@link ApiError} with the relative information
     */
    public static ApiResult<User> register(HttpServletRequest req) {
        return ApiRunner.run(() -> new UserBuilder()
                .addUsername(req.getParameter("username"))
                .addClearPassword(req.getParameter("clearPassword"))
                .addRepeatPassword(req.getParameter("repeatPassword"))
                .addEmail(req.getParameter("email"))
                .addName(req.getParameter("name"))
                .addSurname(req.getParameter("username"))
                .build()
                .flatMap(User::save));
    }
}
