package it.polimi.tiw.api.dbaccess;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Interface for classes that create or retrieve a {@link Connection} from a data source.
 */
interface ConnectionRetriever {
    /**
     * Returns a new {@link Connection}.
     *
     * @return a new database connection from the pool of connections
     * @throws SQLException          if a {@link Connection} cannot be established
     * @throws IllegalStateException if a generic error occurred
     */
    Connection getConnection() throws SQLException;
}
