package it.polimi.tiw.api.dbaccess;

import it.polimi.tiw.api.ApiResult;
import it.polimi.tiw.api.beans.PersistedObject;

/**
 * Represents a generic object that retrieves
 *
 * @param <T> the type of the object handled
 */
public interface DatabaseAccessObject<T extends PersistedObject> {
    /**
     * Saves this object to the database. If the object is already present, it updates the existing one.
     *
     * @param o the object to save
     * @return an {@link ApiResult} containing an error is something went wrong or the saved object is the operation was
     * a success
     */
    ApiResult<T> save(T o);
}
