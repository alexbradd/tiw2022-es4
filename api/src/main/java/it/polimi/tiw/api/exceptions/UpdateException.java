package it.polimi.tiw.api.exceptions;

import it.polimi.tiw.api.dbaccess.DatabaseAccessObject;

import java.sql.SQLException;

/**
 * Represents a database error occurred during a {@link DatabaseAccessObject#save()}. It wraps a given
 * {@link SQLException}.
 */
public class UpdateException extends RuntimeException {
    private final int errorCode;
    private final String errorString;

    /**
     * Constructs a new exception with {@code null} as its detail message.
     * The cause is not initialized, and may subsequently be initialized by a
     * call to {@link #initCause}.
     */
    public UpdateException(SQLException wrapped) {
        super("An error occurred during update: " + wrapped.getMessage(), wrapped);
        errorCode = wrapped.getErrorCode();
        errorString = wrapped.getMessage();
    }

    /**
     * Getter for the SQL error code
     *
     * @return the SQL error code
     */
    public int getErrorCode() {
        return errorCode;
    }

    /**
     * Getter for the SQL error string
     *
     * @return the SQL error string
     */
    public String getErrorString() {
        return errorString;
    }
}
