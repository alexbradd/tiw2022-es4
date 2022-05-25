package it.polimi.tiw.api.dbaccess;

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
}
