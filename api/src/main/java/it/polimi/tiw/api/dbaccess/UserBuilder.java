package it.polimi.tiw.api.dbaccess;

import it.polimi.tiw.api.ApiError;
import it.polimi.tiw.api.ApiResult;
import it.polimi.tiw.api.ApiSubError;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Utility class for constructing a user
 */
public class UserBuilder {
    private String username = null;
    private String password1 = null;
    private String password2 = null;
    private String email = null;
    private String name = null;
    private String surname = null;

    /**
     * default constructor
     */
    public UserBuilder() {
    }

    private UserBuilder(UserBuilder old) {
        this.username = old.username;
        this.password1 = old.password1;
        this.password2 = old.password2;
        this.email = old.email;
        this.name = old.name;
        this.surname = old.surname;
    }

    private static List<ApiSubError> checkRequiredParameter(String param, String paramName) {
        ArrayList<ApiSubError> errors = new ArrayList<>();
        if (param == null)
            errors.add(new ApiSubError("NoSuchElementException", paramName));
        else if (!User.checkLength(param))
            errors.add(new ApiSubError("IllegalArgumentException", paramName + " exceeded maximum length"));
        return errors;
    }

    private static List<ApiSubError> checkRequiredVerifiedParameter(String param, String paramName, Function<String, Boolean> checker) {
        ArrayList<ApiSubError> errors = new ArrayList<>();
        if (param == null)
            errors.add(new ApiSubError("NoSuchElementException", paramName));
        else if (!User.checkLength(param))
            errors.add(new ApiSubError("IllegalArgumentException", paramName + "exceeded maximum length"));
        else if (!checker.apply(param))
            errors.add(new ApiSubError("IllegalArgumentException", "Malformed parameter " + paramName));
        return errors;
    }

    private static List<ApiSubError> passwordVerification(String clear, String repeat) {
        ArrayList<ApiSubError> errors = new ArrayList<>();
        if (clear == null)
            errors.add(new ApiSubError("NoSuchElementException", "clearPassword"));
        if (repeat == null)
            errors.add(new ApiSubError("NoSuchElementException", "repeatPassword"));
        if (!Objects.equals(clear, repeat))
            errors.add(new ApiSubError("IllegalArgumentException", "clearPassword != repeatPassword"));
        return errors;
    }

    /**
     * Returns a new instance with the username added
     *
     * @param username the username to add
     * @return a new instance with the username added
     */
    public UserBuilder addUsername(String username) {
        UserBuilder u = new UserBuilder(this);
        u.username = username;
        return u;
    }

    /**
     * Returns a new instance with the cleartext password added
     *
     * @param clearPassword the cleartext password to add
     * @return a new instance with the cleartext password added
     */
    public UserBuilder addClearPassword(String clearPassword) {
        UserBuilder u = new UserBuilder(this);
        u.password1 = clearPassword;
        return u;
    }

    /**
     * Returns a new instance with the repeated password added
     *
     * @param repeatPassword the repeated password to add
     * @return a new instance with the repeated password added
     */
    public UserBuilder addRepeatPassword(String repeatPassword) {
        UserBuilder u = new UserBuilder(this);
        u.password2 = repeatPassword;
        return u;
    }

    /**
     * Returns a new instance with the email added
     *
     * @param email the email to add
     * @return a new instance with the email added
     */
    public UserBuilder addEmail(String email) {
        UserBuilder u = new UserBuilder(this);
        u.email = email;
        return u;
    }

    /**
     * Returns a new instance with the name added
     *
     * @param name the name to add
     * @return a new instance with the name added
     */
    public UserBuilder addName(String name) {
        UserBuilder u = new UserBuilder(this);
        u.name = name;
        return u;
    }

    /**
     * Returns a new instance with the surname added
     *
     * @param surname the surname to add
     * @return a new instance with the surname added
     */
    public UserBuilder addSurname(String surname) {
        UserBuilder u = new UserBuilder(this);
        u.surname = surname;
        return u;
    }

    /**
     * Tries to construct a new User with the given information. If the construction fails, an ApiResult is
     * returned with the detailed ApiError. Otherwise, an ApiError with the new User is returned.
     *
     * @return an ApiResult representing the state of the operation
     */
    public ApiResult<User> build() {
        ArrayList<ApiSubError> errors = new ArrayList<>();
        errors.addAll(checkRequiredVerifiedParameter(username, "username", User::verifyUsername));
        errors.addAll(passwordVerification(password1, password2));
        errors.addAll(checkRequiredVerifiedParameter(email, "email", User::verifyEmail));
        errors.addAll(checkRequiredParameter(name, "name"));
        errors.addAll(checkRequiredParameter(surname, "surname"));

        if (errors.size() > 0)
            return ApiResult.error(new ApiError(400,
                    "Malformed or missing parameters",
                    errors.toArray(new ApiSubError[0])));
        return ApiResult.ok(new User(username, password1, email, name, surname));
    }
}
