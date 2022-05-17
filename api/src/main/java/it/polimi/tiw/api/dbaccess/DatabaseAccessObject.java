package it.polimi.tiw.api.dbaccess;

import it.polimi.tiw.api.beans.PersistedObject;
import it.polimi.tiw.api.functional.ApiResult;

/**
 * Represents a generic object that retrieves
 *
 * @param <T> the type of the object handled
 */
public interface DatabaseAccessObject<T extends PersistedObject> {
    /**
     * Finds and retrieves the data for the object with the given id. If no such object can be found, an empty
     * {@link ApiResult} is returned.
     *
     * @param base64Id the id to search
     * @return an {@link ApiResult} containing the constructed object
     */
    ApiResult<T> byId(String base64Id);

    /**
     * Inserts this object into the database. If the object is already present, it returns an error, otherwise the object
     * inserted.
     * <p>
     * The operation will be done atomically using transactions. If automatic transaction management has been turned
     * off, e.g. with {@link java.sql.Connection#setAutoCommit(boolean)}, it is the caller's responsibility to commit
     * or rollback the changes.
     *
     * @param o the object to insert
     * @return an {@link ApiResult} containing an error or the saved object
     */
    ApiResult<T> insert(T o);

    /**
     * Updates the entity corresponding to this object in the database. If the object is not present in the database,
     * an error is returned. Otherwise, the object passed is returned.
     * <p>
     * The operation will be done atomically using transactions. If automatic transaction management has been turned
     * off, e.g. with {@link java.sql.Connection#setAutoCommit(boolean)}, it is the caller's responsibility to commit
     * or rollback the changes.
     *
     * @param o the object to update
     * @return an {@link ApiResult} containing an error or the updated object
     */
    ApiResult<T> update(T o);

    /**
     * Checks whether the given object is stored in the database or not
     *
     * @param o the object to check
     * @return true is the given object has a correspondent in the database
     */
    boolean isPersisted(T o);
}
