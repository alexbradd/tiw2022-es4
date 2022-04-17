package it.polimi.tiw.api.dbaccess;

import it.polimi.tiw.api.exceptions.UnavailableDatabaseException;

import java.sql.Connection;

/**
 * Interface for classes that create or retrieve a {@link Connection} from a data source.
 */
interface ConnectionRetriever {
    /**
     * Returns a new {@link Connection}.
     *
     * @return a new database connection from the pool of connections
     * @throws UnavailableDatabaseException if a {@link Connection} cannot be established
     * @throws IllegalStateException        if a generic error occurred
     */
    Connection getConnection() throws UnavailableDatabaseException;
}
