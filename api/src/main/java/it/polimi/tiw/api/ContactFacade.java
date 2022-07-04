package it.polimi.tiw.api;

import it.polimi.tiw.api.beans.Contact;
import it.polimi.tiw.api.beans.User;
import it.polimi.tiw.api.dbaccess.ContactDAO;
import it.polimi.tiw.api.functional.ApiResult;

import java.sql.Connection;
import java.util.List;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/**
 * Class exposing a simple interface for manipulating {@link Contact}s
 */
public class ContactFacade {
    private final Connection connection;
    private final Function<Connection, ContactDAO> contactDAOGenerator;

    /**
     * Constructs a new instance with the given dependencies
     *
     * @param connection          the {@link Connection}
     * @param contactDAOGenerator a {@link Function} creating a new {@link ContactDAO} using the passed connection
     */
    public ContactFacade(Connection connection, Function<Connection, ContactDAO> contactDAOGenerator) {
        this.connection = requireNonNull(connection);
        this.contactDAOGenerator = requireNonNull(contactDAOGenerator);
    }

    /**
     * Stores a new {@link Contact} with the given owner and contact in the database.
     *
     * @param ownerId   the base64 id of the {@link User} owner
     * @param contactId the base64 id of the {@link User} contact
     * @return an {@link ApiResult} containing the newly created object or an error, if one occured
     */
    public ApiResult<Contact> saveContact(String ownerId, String contactId) {
        Contact c = new Contact();
        c.setOwnerBase64Id(ownerId);
        c.setContactBase64Id(contactId);
        return contactDAOGenerator.apply(connection).insert(c);
    }

    /**
     * Fetches all contacts owned by the {@link User} with the given id
     *
     * @param userId the user's base64 id
     * @return an {@link ApiResult} containing the list of {@link Contact} associated with this user or an error if
     * one happened
     */
    public ApiResult<List<Contact>> ofUser(String userId) {
        return contactDAOGenerator.apply(connection).ofUser(userId);
    }

    /**
     * Creates a new ContactFacade using the default objects
     *
     * @param connection The {@link Connection} to use
     * @return a new ContactFacade
     * @throws NullPointerException if parameter is null
     */
    public static ContactFacade withDefaultObjects(Connection connection) {
        return new ContactFacade(requireNonNull(connection), ContactDAO::withNewObjects);
    }
}
