package it.polimi.tiw.api.beans;

import it.polimi.tiw.api.ApiError;
import it.polimi.tiw.api.ApiResult;
import it.polimi.tiw.api.ApiSubError;
import it.polimi.tiw.api.functional.Either;
import it.polimi.tiw.api.functional.Tuple;
import it.polimi.tiw.api.utils.PasswordUtils;

import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;

public class User implements PersistedObject {
    private String username;
    private String saltedPassword;
    private String email;
    private String name;
    private String surname;

    private User(Builder builder) {
        this.username = builder.username;
        this.saltedPassword = builder.saltedPassword;
        this.email = builder.email;
        this.name = builder.name;
        this.surname = builder.surname;
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

    public static class Builder {
        /**
         * Maximum allowed string length
         */
        public static final int STRING_LENGTH = 128;
        /**
         * The {@link Pattern} used for validating correctness of the usernames passed.
         */
        public static final Pattern USERNAME_REGEX = Pattern.compile("^[a-z0-9_-]{3,20}$", Pattern.CASE_INSENSITIVE);
        /**
         * The {@link Pattern} used for validating correctness of the emails passed.
         */
        public static final Pattern EMAIL_REGEX = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}",
                Pattern.CASE_INSENSITIVE);
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
                l.add(new ApiSubError("IllegalArgumentException", paramName + " exceeded maximum length"));
                return false;
            }
            return true;
        }

        private static void checkRequiredParameter(String param, String paramName, Function<String, Boolean> checker, ArrayList<ApiSubError> l) {
            if (checkRequiredParameter(param, paramName, l) && !checker.apply(param))
                l.add(new ApiSubError("IllegalArgumentException", "Malformed parameter " + paramName));
        }

        /**
         * Checks that the given string is under the maximum string length
         */
        private static boolean checkLength(String s) {
            return s.length() <= STRING_LENGTH;
        }

        /**
         * Verify that the passed string is a valid username
         *
         * @param username the string to check
         * @return true if the passed username is a valid username
         */
        private static boolean verifyUsername(String username) {
            return username != null && USERNAME_REGEX.matcher(username).find();
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
            checkRequiredParameter(username, "username", Builder::verifyUsername, missing);
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
