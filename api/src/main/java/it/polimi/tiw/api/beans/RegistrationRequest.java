package it.polimi.tiw.api.beans;

/**
 * Bean representing a request for creating a new {@link User} object.
 */
public class RegistrationRequest {
    private String username;
    private String clearPassword;
    private String repeatPassword;
    private String email;
    private String name;
    private String surname;

    /**
     * Returns the username associated with this request.
     *
     * @return the username associated with this request
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the username associated with this request.
     *
     * @param username the username associated with this request.
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Returns the password associated with this request.
     *
     * @return the password associated with this request
     */
    public String getClearPassword() {
        return clearPassword;
    }

    /**
     * Sets the password associated with this request.
     *
     * @param clearPassword the password associated with this request.
     */
    public void setClearPassword(String clearPassword) {
        this.clearPassword = clearPassword;
    }

    /**
     * Returns the repeat of the password associated with this request.
     *
     * @return the repeat of the password associated with this request
     */
    public String getRepeatPassword() {
        return repeatPassword;
    }

    /**
     * Sets the repeat of the password associated with this request.
     *
     * @param repeatPassword the repeat of the password associated with this request.
     */
    public void setRepeatPassword(String repeatPassword) {
        this.repeatPassword = repeatPassword;
    }

    /**
     * Returns the email associated with this request.
     *
     * @return the email associated with this request
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the email associated with this request.
     *
     * @param email the email associated with this request.
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Returns the name associated with this request.
     *
     * @return the name associated with this request
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name associated with this request.
     *
     * @param name the name associated with this request.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the surname associated with this request.
     *
     * @return the surname associated with this request
     */
    public String getSurname() {
        return surname;
    }

    /**
     * Sets the surname associated with this request.
     *
     * @param surname the name associated with this request.
     */
    public void setSurname(String surname) {
        this.surname = surname;
    }
}
