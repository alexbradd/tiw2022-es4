package it.polimi.tiw.api.beans;

/**
 * Interface for an object that is saved to database.
 */
public interface PersistedObject {
    /**
     * Returns true if this instance has any null properties. Id is excluded.
     *
     * @return true if this instance has any null properties
     */
    default boolean hasNullProperties() {
        return hasNullProperties(false);
    }

    /**
     * Returns true if this instance has any null properties.
     *
     * @param includeId flag for indicating if the id should be included or not
     * @return true if this instance has any null properties.
     */
    boolean hasNullProperties(boolean includeId);
}
