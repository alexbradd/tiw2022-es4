package it.polimi.tiw.api.beans;

/**
 * Bean representing a request for logging in a new user.
 */
public class LoginRequest {
    private String username;
    private String clearPassword;

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
}
