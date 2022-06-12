package it.polimi.tiw.api.beans;

/**
 * Bean representing a contact.
 */
public class Contact implements PersistedObject {
    private String ownerBase64Id;
    private String contactBase64Id;

    /**
     * Getter for the base64 encoded id of the owner of this contact.
     *
     * @return the base64 encoded id of the owner of this contact.
     */
    public String getOwnerBase64Id() {
        return ownerBase64Id;
    }

    /**
     * Sets the base64 encoded id of the owner of this contact.
     *
     * @param ownerBase64Id the new base64 encoded id of the owner of this contact
     */
    public void setOwnerBase64Id(String ownerBase64Id) {
        this.ownerBase64Id = ownerBase64Id;
    }

    /**
     * Getter for the base64 encoded id of the user this contact refers to.
     *
     * @return the base64 encoded id of the user this contact refers to
     */
    public String getContactBase64Id() {
        return contactBase64Id;
    }

    /**
     * Sets the base64 encoded id of the user this contact refresh to.
     *
     * @param contactBase64Id the new base64 encoded id of the user this contact refresh to
     */
    public void setContactBase64Id(String contactBase64Id) {
        this.contactBase64Id = contactBase64Id;
    }

    /**
     * Returns true if this instance has any null properties.
     *
     * @param includeId ignored
     * @return true if this instance has any null properties.
     */
    @Override
    public boolean hasNullProperties(boolean includeId) {
        return ownerBase64Id == null || contactBase64Id == null;
    }
}
