package it.polimi.tiw.api.dbaccess;

import it.polimi.tiw.api.beans.Contact;
import it.polimi.tiw.api.beans.User;
import it.polimi.tiw.api.error.ApiError;
import it.polimi.tiw.api.error.ApiSubError;
import it.polimi.tiw.api.error.Errors;
import it.polimi.tiw.api.functional.ApiResult;
import it.polimi.tiw.api.utils.IdUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

/**
 * Class for retrieving/sending {@link Contact} beans to the database
 */
public class ContactDAO implements DatabaseAccessObject<Contact> {
    private static final ApiError operationNotPermitted = new ApiError(400,
            "Operation not permitted",
            new ApiSubError("UnsupportedOperationException", "The operation requested is not allowed"));

    private final Connection connection;
    private final UserDAO userDAO;

    /**
     * Creates a new ContactDAO using the given {@link Connection} and {@link UserDAO} instances.
     *
     * @param connection the {@link Connection}
     * @param userDAO    the {@link UserDAO}
     * @throws NullPointerException if any parameter is null
     */
    public ContactDAO(Connection connection, UserDAO userDAO) {
        this.connection = requireNonNull(connection);
        this.userDAO = requireNonNull(userDAO);
    }

    /**
     * Always returns an error since a {@link Contact} does not have a queryable ID.
     *
     * @param base64Id the id to search
     * @return an {@link ApiResult} containing the constructed object
     */
    @Override
    public ApiResult<Contact> byId(String base64Id) {
        if (isNull(base64Id)) return ApiResult.error(Errors.fromNullParameter("base64Id"));
        return ApiResult.error(operationNotPermitted);
    }

    /**
     * Finds and retrieves all {@link Contact}s relative to the {@link User} with the given id.
     *
     * @param base64Id the id of the user
     * @return an {@link ApiResult} containing the list of contacts or an error if one happened
     */
    public ApiResult<List<Contact>> ofUser(String base64Id) {
        if (isNull(base64Id)) return ApiResult.error(Errors.fromNullParameter("base64Id"));
        if (!IdUtils.isValidBase64(base64Id)) return ApiResult.error(Errors.fromMalformedParameter("base64Id"));
        try {
            String sql = "select ownerId, contactId from tiw_app.contacts where ownerId = ?";
            try (PreparedStatement s = connection.prepareStatement(sql)) {
                s.setLong(1, IdUtils.fromBase64(base64Id));
                try (ResultSet r = s.executeQuery()) {
                    List<Contact> contacts = new ArrayList<>();
                    while (r.next()) {
                        Contact c = new Contact();
                        c.setOwnerBase64Id(IdUtils.toBase64(r.getLong(1)));
                        c.setContactBase64Id(IdUtils.toBase64(r.getLong(2)));
                        contacts.add(c);
                    }
                    return ApiResult.ok(contacts);
                }
            }
        } catch (SQLException e) {
            return ApiResult.error(Errors.fromSQLException(e));
        }
    }

    /**
     * Inserts this object into the database. If the object is already present, it returns an error, otherwise the object
     * inserted.
     * <p>
     * The operation will be done atomically using transactions. If automatic transaction management has been turned
     * off, e.g. with {@link Connection#setAutoCommit(boolean)}, it is the caller's responsibility to commit
     * or rollback the changes.
     *
     * @param contact the object to insert
     * @return an {@link ApiResult} containing an error or the saved object
     */
    @Override
    public ApiResult<Contact> insert(Contact contact) {
        if (isNull(contact)) return ApiResult.error(Errors.fromNullParameter("contact"));
        if (contact.hasNullProperties(false)) return ApiResult.error(Errors.fromMalformedParameter("contact"));
        if (Objects.equals(contact.getContactBase64Id(), contact.getOwnerBase64Id()))
            return ApiResult.error(Errors.fromMalformedParameter("contact"));
        if (!IdUtils.isValidBase64(contact.getContactBase64Id()) || !IdUtils.isValidBase64(contact.getOwnerBase64Id()))
            return ApiResult.error(Errors.fromMalformedParameter("contact"));
        if (isPersisted(contact))
            return ApiResult.error(Errors.fromConflict("contact"));

        try {
            String sql = "insert into tiw_app.contacts(ownerId, contactId) values (?, ?)";
            boolean prevAutocommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                long ownerId = IdUtils.fromBase64(contact.getOwnerBase64Id());
                long contactId = IdUtils.fromBase64(contact.getContactBase64Id());

                boolean validUsers = userDAO.byId(ownerId)
                        .then(() -> userDAO.byId(contactId))
                        .match(__ -> true, __ -> false);
                if (!validUsers) {
                    if (prevAutocommit) connection.rollback();
                    return ApiResult.error(Errors.fromConflict("contact"));
                }

                try (PreparedStatement s = connection.prepareStatement(sql)) {
                    s.setLong(1, ownerId);
                    s.setLong(2, contactId);
                    s.executeUpdate();
                }
                if (prevAutocommit) connection.commit();
                return ApiResult.ok(contact);
            } catch (SQLException e) {
                if (prevAutocommit) connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(prevAutocommit);
            }
        } catch (SQLException e) {
            return ApiResult.error(Errors.fromSQLException(e));
        }
    }

    /**
     * Always returns an error since a {@link Contact} cannot be updated.
     *
     * @param contact the object to update
     * @return an {@link ApiResult} containing an error or the updated object
     */
    @Override
    public ApiResult<Contact> update(Contact contact) {
        if (isNull(contact)) return ApiResult.error(Errors.fromNullParameter("contact"));
        return ApiResult.error(operationNotPermitted);
    }

    /**
     * Checks whether the given object is stored in the database or not
     *
     * @param o the object to check
     * @return true is the given object has a correspondent in the database
     */
    @Override
    public boolean isPersisted(Contact o) {
        if (isNull(o)) return false;
        if (o.hasNullProperties()) return false;
        if (!IdUtils.isValidBase64(o.getOwnerBase64Id())) return false;
        if (!IdUtils.isValidBase64(o.getContactBase64Id())) return false;

        String sql = "select * from tiw_app.contacts where ownerId = ? and contactId = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, IdUtils.fromBase64(o.getOwnerBase64Id()));
            statement.setLong(2, IdUtils.fromBase64(o.getContactBase64Id()));
            try (ResultSet results = statement.executeQuery()) {
                return results.next();
            }
        } catch (SQLException ignored) {
            return false;
        }
    }

    /**
     * Returns a new ContactDAO object that will create all the DAO objects it
     * needs from scratch sharing the given {@link Connection}.
     *
     * @param connection a {@link Connection}
     * @return a new ContactDAO object
     * @throws NullPointerException if {@code connection} is null
     */
    public static ContactDAO withNewObjects(Connection connection) {
        requireNonNull(connection);
        return new ContactDAO(connection, new UserDAO(connection));
    }
}
