package it.polimi.tiw.api.dbaccess;

import it.polimi.tiw.api.ApiError;
import it.polimi.tiw.api.ApiSubError;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Some internal utilities
 */
class DAOUtils {
    /**
     * Mocks mysql's AutoIncrement
     */
    static long genNewId(Connection connection, String table, String idColumn) throws SQLException {
        try (PreparedStatement p = connection.prepareStatement("select max(" + idColumn + ") as id from " + table)) {
            try (ResultSet r = p.executeQuery()) {
                r.next();
                return r.getLong("id") + 1;
            }
        }
    }

    public static ApiError fromSQLException(SQLException e) {
        return new ApiError(500,
                "Error while fetching data",
                new ApiSubError("SQLException", e.getMessage() == null ? "" : e.getMessage()));
    }

    public static ApiError fromMissingElement(String specifics) {
        return new ApiError(404,
                "Cannot find this object",
                new ApiSubError("NoSuchElementException", "No such with " + specifics));
    }

    public static ApiError fromNullParameter(String param) {
        return new ApiError(400,
                "Required parameter is missing",
                new ApiSubError("NullPointerException", "Required parameter " + param + " is null"));
    }

    public static ApiError fromMalformedParameter(String param) {
        return new ApiError(400,
                "Parameter is malformed",
                new ApiSubError("IllegalArgumentException", "Parameter " + param + " is of invalid format"));
    }
}
