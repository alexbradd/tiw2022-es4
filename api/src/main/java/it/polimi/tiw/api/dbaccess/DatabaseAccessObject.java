package it.polimi.tiw.api.dbaccess;

import it.polimi.tiw.api.ApiResult;

/**
 * Represents a generic object that models an entity in the database.
 */
public interface DatabaseAccessObject {
    /**
     * Saves this object to the database. If the object is already present, it updates the existing one.
     */
    ApiResult<? extends DatabaseAccessObject> save();
}
