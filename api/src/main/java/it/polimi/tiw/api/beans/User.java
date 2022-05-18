package it.polimi.tiw.api.beans;

import it.polimi.tiw.api.error.ApiError;
import it.polimi.tiw.api.error.ApiSubError;
import it.polimi.tiw.api.functional.ApiResult;
import it.polimi.tiw.api.functional.Either;
import it.polimi.tiw.api.functional.Tuple;
import it.polimi.tiw.api.utils.PasswordUtils;

import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;

import static java.util.Objects.isNull;

/**
 * Bean representing a User
 */
public class User implements PersistedObject {
    private String base64Id;
    private String username;
    private String saltedPassword;
    private String email;
    private String name;
    private String surname;

    private User(Builder builder) {
        this.base64Id = builder.base64Id;
        this.username = builder.username;
        this.saltedPassword = builder.saltedPassword;
        this.email = builder.email;
        this.name = builder.name;
        this.surname = builder.surname;
    }

    /**
     * Getter for this User's id. The returned id might be null, e.g. when the User's hasn't been saved to database.
     *
     * @return this User's id
     */
    public String getBase64Id() {
        return base64Id;
    }

    /**
     * Setter for the id of this User
     *
     * @param base64Id the new id
     */
    public void setBase64Id(String base64Id) {
        this.base64Id = base64Id;
    }

    /**
     * Getter for the User's username
     *
     * @return the User's username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets this User's username to the specified value.
     *
     * @param username the new username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Getter for this User's hashed + salted password.
     *
     * @return this User's hashed + salted password.
     */
    public String getSaltedPassword() {
        return saltedPassword;
    }

    /**
     * Sets this User's salted password
     *
     * @param saltedPassword the salted password
     */
    public void setSaltedPassword(String saltedPassword) {
        this.saltedPassword = saltedPassword;
    }

    /**
     * Getter for this User's email
     *
     * @return this User's email
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets this User's email to the given one
     *
     * @param email the new email
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Getter for this User's name
     *
     * @return this User's name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets this User's name to the given one
     *
     * @param name the new name
     * @throws NullPointerException     if {@code name} is null
     * @throws IllegalArgumentException if {@code name} is too long
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Getter for this User's surname
     *
     * @return this User's surname
     */
    public String getSurname() {
        return surname;
    }

    /**
     * Sets this User's name to the given one
     *
     * @param surname the new name
     */
    public void setSurname(String surname) {
        this.surname = surname;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasNullProperties(boolean includeId) {
        return (isNull(base64Id) && includeId) ||
                isNull(username) ||
                isNull(saltedPassword) ||
                isNull(email) ||
                isNull(name) ||
                isNull(surname);
    }

    public static class Builder {
        /**
         * Maximum allowed string length
         */
        public static final int STRING_LENGTH = 128;
        /**
         * Maximum allowed salted password length
         */
        public static final int SALTED_PASSWORD_LENGTH = 512;
        /**
         * The {@link Pattern} used for validating correctness of the emails passed.
         */
        public static final Pattern EMAIL_REGEX = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}",
                Pattern.CASE_INSENSITIVE);
        private String base64Id = null;
        private String username = null;
        private Either<String, Tuple<String, String>> password = null;
        private String saltedPassword = null;
        private String email = null;
        private String name = null;
        private String surname = null;

        /**
         * default constructor
         */
        public Builder() {
        }

        private Builder(Builder old) {
            this.base64Id = old.base64Id;
            this.username = old.username;
            this.password = old.password;
            this.saltedPassword = old.saltedPassword;
            this.email = old.email;
            this.name = old.name;
            this.surname = old.surname;
        }

        private static boolean checkRequiredParameter(String param, String paramName, ArrayList<ApiSubError> l) {
            if (param == null) {
                l.add(new ApiSubError("NoSuchElementException", paramName));
                return false;
            } else if (!checkLength(param)) {
                l.add(new ApiSubError("IllegalArgumentException", paramName + " has invalid length"));
                return false;
            }
            return true;
        }

        private static void checkRequiredParameter(String param, String paramName, Function<String, Boolean> checker, ArrayList<ApiSubError> l) {
            if (checkRequiredParameter(param, paramName, l) && !checker.apply(param))
                l.add(new ApiSubError("IllegalArgumentException", "Malformed parameter " + paramName));
        }

        /**
         * Checks that the given string is greater than 0 and under the maximum string length
         */
        private static boolean checkLength(String s) {
            return checkLength(s, STRING_LENGTH);
        }

        /**
         * Checks that the given string is greater than 0 and under the given length
         */
        private static boolean checkLength(String s, int length) {
            return s.length() > 0 && s.length() <= length;
        }

        /**
         * Verify that the passed string is a valid email
         *
         * @param email the string to check
         * @return true if the passed email is a valid email
         */
        private static boolean verifyEmail(String email) {
            return email != null && EMAIL_REGEX.matcher(email).find();
        }

        private static void checkPassword(Either<String, Tuple<String, String>> password, ArrayList<ApiSubError> l) {
            if (password == null) {
                l.add(new ApiSubError("NoSuchElementException", "Missing password"));
                return;
            }
            ArrayList<ApiSubError> err = password.match(
                    (String s) -> {
                        ArrayList<ApiSubError> e = new ArrayList<>();
                        if (s == null)
                            e.add(new ApiSubError("NoSuchElementException", "saltedPassword"));
                        else if (!checkLength(s, SALTED_PASSWORD_LENGTH))
                            e.add(new ApiSubError("IllegalArgumentException", "saltedPassword is of invalid length"));
                        return e;
                    },
                    (Tuple<String, String> t) -> {
                        ArrayList<ApiSubError> e = new ArrayList<>();
                        checkRequiredParameter(t.getFirst(), "clearPassword", e);
                        checkRequiredParameter(t.getSecond(), "repeatPassword", e);
                        if (!Objects.equals(t.getFirst(), t.getSecond()))
                            e.add(new ApiSubError("IllegalArgumentException", "clearPassword != repeatPassword"));
                        return e;
                    }
            );
            l.addAll(err);
        }

        /**
         * Returns a new instance with the given base64 id added
         *
         * @param base64Id the url safe base64 encoded id
         * @return a new instance with the id added
         */
        public Builder addId(String base64Id) {
            Builder u = new Builder(this);
            u.base64Id = base64Id;
            return u;
        }

        /**
         * Returns a new instance with the username added
         *
         * @param username the username to add
         * @return a new instance with the username added
         */
        public Builder addUsername(String username) {
            Builder u = new Builder(this);
            u.username = username;
            return u;
        }

        /**
         * Returns a new instance with a new password calculated from the given clear text ones
         *
         * @param clearPassword  the cleartext password to add
         * @param repeatPassword the clearTest password to add
         * @return a new instance with the password added
         */
        public Builder addPassword(String clearPassword, String repeatPassword) {
            Builder u = new Builder(this);
            u.password = Either.right(new Tuple<>(clearPassword, repeatPassword));
            if (clearPassword != null)
                u.saltedPassword = PasswordUtils.toHash(clearPassword);
            return u;
        }

        /**
         * Returns a new instance with a given salted and hashed password
         *
         * @param saltedPassword the already salted and hashed password
         * @return a new instance with the password added
         */
        public Builder addPassword(String saltedPassword) {
            Builder u = new Builder(this);
            u.password = Either.left(saltedPassword);
            u.saltedPassword = saltedPassword;
            return u;
        }

        /**
         * Returns a new instance with the email added
         *
         * @param email the email to add
         * @return a new instance with the email added
         */
        public Builder addEmail(String email) {
            Builder u = new Builder(this);
            u.email = email;
            return u;
        }

        /**
         * Returns a new instance with the name added
         *
         * @param name the name to add
         * @return a new instance with the name added
         */
        public Builder addName(String name) {
            Builder u = new Builder(this);
            u.name = name;
            return u;
        }

        /**
         * Returns a new instance with the surname added
         *
         * @param surname the surname to add
         * @return a new instance with the surname added
         */
        public Builder addSurname(String surname) {
            Builder u = new Builder(this);
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
            ArrayList<ApiSubError> missing = new ArrayList<>();
            checkRequiredParameter(username, "username", missing);
            checkPassword(password, missing);
            checkRequiredParameter(email, "email", Builder::verifyEmail, missing);
            checkRequiredParameter(name, "name", missing);
            checkRequiredParameter(surname, "surname", missing);
            if (missing.size() > 0)
                return ApiResult.error(new ApiError(400,
                        "Malformed or missing parameters",
                        missing.toArray(new ApiSubError[0])));
            return ApiResult.ok(new User(this));
        }
    }
}
