package it.polimi.tiw.api.dbaccess;

import it.polimi.tiw.api.ApiResult;

import java.sql.Connection;
import java.util.function.Function;

/**
 * Interface for classes that create or retrieve a {@link Connection} from a data source.
 */
public interface ConnectionRetriever {
    /**
     * Returns a new {@link ApiResult} containing a new {@link Connection} to the database.
     *
     * @return a new database connection from the pool of connections
     * @throws IllegalStateException if a generic error occurred
     */
    ApiResult<Connection> get();

    /**
     * Executes the given {@link Function} with a connection retrieved from database. After execution, it closes the
     * connection
     *
     * @param mapper the {@link Function} to execute
     * @param <T>    the type of the element contained inside the {@link ApiResult}
     * @return an {@link ApiResult} containing a new value or an error.
     */
    <T> ApiResult<T> with(Function<Connection, ApiResult<T>> mapper);

    /**
     * Tries to close the given connection to the database
     *
     * @param connection the connection to close
     */
    void close(Connection connection);
}
