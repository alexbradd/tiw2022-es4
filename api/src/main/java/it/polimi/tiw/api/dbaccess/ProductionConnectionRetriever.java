package it.polimi.tiw.api.dbaccess;

import it.polimi.tiw.api.ApiError;
import it.polimi.tiw.api.ApiResult;
import it.polimi.tiw.api.ApiSubError;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Function;

/**
 * Singleton that gets a new connection from the pool of connections to the production database.
 */
public class ProductionConnectionRetriever implements ConnectionRetriever {
    private static ProductionConnectionRetriever instance;
    private DataSource ds;

    private ProductionConnectionRetriever() {
    }

    /**
     * Returns the singleton instance
     *
     * @return the singleton instance
     */
    public static ProductionConnectionRetriever getInstance() {
        if (instance == null)
            instance = new ProductionConnectionRetriever();
        return instance;
    }

    /**
     * Returns a new {@link ApiResult} containing a new {@link Connection} to the database.
     *
     * @return a new database connection from the pool of connections
     * @throws IllegalStateException if a generic error occurred
     */
    public ApiResult<Connection> get() {
        ProductionConnectionRetriever i = getInstance();
        try {
            if (i.ds == null) {
                InitialContext ctx = new InitialContext();
                i.ds = (DataSource) ctx.lookup("java:/comp/env/jdbc/productionDb");
            }
            return ApiResult.ok(i.ds.getConnection());
        } catch (NamingException e) {
            throw new IllegalStateException("jndi i setup improperly", e);
        } catch (SQLException e) {
            ApiError error = new ApiError(500,
                    "Error while fetching data",
                    new ApiSubError("SQLException", e.getMessage() == null ? "" : e.getMessage()));
            return ApiResult.error(error);
        }
    }

    /**
     * Executes the given {@link Function} with a connection retrieved from database. After execution, it closes the
     * connection
     *
     * @param mapper the {@link Function} to execute
     * @param <T>    the type of the element contained inside the {@link ApiResult}
     * @return an {@link ApiResult} containing a new value or an error.
     */
    @Override
    public <T> ApiResult<T> with(Function<Connection, ApiResult<T>> mapper) {
        ApiResult<Connection> conn = getInstance().get();
        ApiResult<T> res = conn.flatMap(mapper);
        return conn.match(c -> {
            getInstance().close(c);
            return res;
        }, ApiResult::error);
    }

    /**
     * Tries to close the given connection to the database
     *
     * @param connection the connection to close
     */
    @Override
    public void close(Connection connection) {
        try {
            connection.close();
        } catch (SQLException ignored) {
        }
    }
}
