package it.polimi.tiw.api.dbaccess;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Singleton that gets a new connection from the pool of connections to the production database.
 */
class ProductionConnectionRetriever implements ConnectionRetriever {
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
     * Returns a new {@link Connection} from a pool of connections.
     *
     * @return a new database connection from the pool of connections
     * @throws SQLException          if a {@link Connection} cannot be established
     * @throws IllegalStateException if a generic error occurred
     */
    public Connection getConnection() throws SQLException {
        ProductionConnectionRetriever i = getInstance();
        try {
            if (i.ds == null) {
                InitialContext ctx = new InitialContext();
                i.ds = (DataSource) ctx.lookup("java:/comp/env/jdbc/productionDb");
            }
            return i.ds.getConnection();
        } catch (NamingException e) {
            throw new IllegalStateException("jndi i setup improperly", e);
        }
    }
}
