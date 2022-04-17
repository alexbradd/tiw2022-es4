package it.polimi.tiw.api.dbaccess;

import it.polimi.tiw.api.exceptions.UnavailableDatabaseException;
import it.polimi.tiw.api.exceptions.UpdateException;

/**
 * Represents a generic object that models an entity in the database.
 */
public interface DatabaseAccessObject {
    /**
     * Saves this object to the database. If the object is already present, it updates the existing one.
     *
     * @throws UnavailableDatabaseException if the operation fails due to database unavailability
     * @throws UpdateException              if an error occurs
     */
    void save();
}
