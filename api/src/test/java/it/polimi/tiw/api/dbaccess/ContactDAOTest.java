package it.polimi.tiw.api.dbaccess;

import it.polimi.tiw.api.beans.Contact;
import it.polimi.tiw.api.beans.User;
import it.polimi.tiw.api.error.Errors;
import it.polimi.tiw.api.functional.ApiResult;
import it.polimi.tiw.api.utils.IdUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContactDAOTest {
    @Mock
    private Connection connection;
    @Mock
    private PreparedStatement statement;
    @Mock
    private ResultSet results;

    @BeforeEach
    void setupMocks() throws SQLException {
        Mockito.lenient().when(connection.prepareStatement(any(String.class))).thenReturn(statement);
        Mockito.lenient().when(statement.executeUpdate()).thenReturn(1);
        Mockito.lenient().when(statement.executeQuery()).thenReturn(results);
    }

    @Test
    void all_withNull() {
        assertThrows(NullPointerException.class, () -> new ContactDAO(null, null));
        assertThrows(NullPointerException.class, () -> new ContactDAO(connection, null));
        ContactDAO.withNewObjects(connection).insert(null).consume(a -> fail(), e -> {
        });
        ContactDAO.withNewObjects(connection).update(null).consume(a -> fail(), e -> {
        });
        ContactDAO.withNewObjects(connection).byId(null).consume(a -> fail(), e -> {
        });
        ContactDAO.withNewObjects(connection).ofUser(null).consume(a -> fail(), e -> {
        });
        assertFalse(ContactDAO.withNewObjects(connection).isPersisted(null));
    }

    @Test
    void byId_unsupported() {
        ContactDAO.withNewObjects(connection).byId("asdfa").consume(a -> fail(), e -> {
        });
        ContactDAO.withNewObjects(connection).byId("AAAAAAAAAAA").consume(a -> fail(), e -> {
        });
    }

    @ParameterizedTest
    @MethodSource("isPersisted_invalidContactSource")
    void isPersisted_invalidTransfer(Contact invalid) throws SQLException {
        Mockito.lenient().when(results.next()).thenReturn(false);
        assertFalse(ContactDAO.withNewObjects(connection).isPersisted(invalid));
    }

    static Stream<Contact> isPersisted_invalidContactSource() {
        Contact invalidId = mock(Contact.class, "invalidId");
        Contact notInDb = mock(Contact.class, "notInDb");

        when(notInDb.getOwnerBase64Id()).thenReturn(IdUtils.toBase64(0L));
        when(notInDb.getContactBase64Id()).thenReturn(IdUtils.toBase64(1L));
        return Stream.of(invalidId, notInDb);
    }

    @Test
    void isPersisted_inDb() throws SQLException {
        Contact a = mock(Contact.class);
        ContactDAO dao = ContactDAO.withNewObjects(connection);
        when(a.getOwnerBase64Id()).thenReturn(IdUtils.toBase64(0L));
        when(a.getContactBase64Id()).thenReturn(IdUtils.toBase64(1L));
        when(results.next()).thenReturn(true);
        assertTrue(dao.isPersisted(a));
    }

    @Test
    void update_unsupported() {
        Contact c = mock(Contact.class);
        ContactDAO.withNewObjects(connection)
                .update(c)
                .consume(__ -> fail(), __ -> {
                });
    }

    @ParameterizedTest
    @MethodSource("insert_invalidContactSource")
    void insert_withInvalidContact(Contact invalid) {
        ContactDAO dao = ContactDAO.withNewObjects(connection);
        dao.insert(invalid).consume(__ -> fail(), __ -> {
        });
    }

    static Stream<Contact> insert_invalidContactSource() {
        Contact withNull = mock(Contact.class, "withNull");
        Contact withInvalidOwnerId = mock(Contact.class, "withInvalidOwnerId");
        Contact withInvalidContactId = mock(Contact.class, "withInvalidContactId");
        Contact withEqualOwnerContact = mock(Contact.class, "withEqualOwnerContact");

        when(withNull.hasNullProperties(anyBoolean())).thenReturn(true);

        when(withInvalidOwnerId.getOwnerBase64Id()).thenReturn("asdf");
        when(withInvalidOwnerId.getContactBase64Id()).thenReturn(IdUtils.toBase64(0L));

        when(withInvalidContactId.getOwnerBase64Id()).thenReturn(IdUtils.toBase64(0L));
        when(withInvalidContactId.getContactBase64Id()).thenReturn("asdf");

        when(withEqualOwnerContact.getOwnerBase64Id()).thenReturn(IdUtils.toBase64(0L));
        when(withEqualOwnerContact.getContactBase64Id()).thenReturn(IdUtils.toBase64(0L));

        return Stream.of(withNull, withInvalidOwnerId, withInvalidContactId, withEqualOwnerContact);
    }

    @Test
    void insert_withAlreadyPersistedContact() {
        Contact mock = mock(Contact.class);
        ContactDAO dao = spy(ContactDAO.withNewObjects(connection));

        when(mock.hasNullProperties(anyBoolean())).thenReturn(false);
        when(mock.hasNullProperties(anyBoolean())).thenReturn(false);
        when(mock.getOwnerBase64Id()).thenReturn(IdUtils.toBase64(0L));
        when(mock.getContactBase64Id()).thenReturn(IdUtils.toBase64(1L));

        doReturn(true).when(dao).isPersisted(mock);
        dao.insert(mock).consume(__ -> fail(), __ -> {
        });
    }

    @Test
    void insert_withNonPresentUser() {
        UserDAO mockUserDAO = mock(UserDAO.class);
        Contact mockContact = mock(Contact.class);
        ContactDAO dao = spy(new ContactDAO(connection, mockUserDAO));

        when(mockUserDAO.byId(0L)).thenReturn(ApiResult.error(Errors.fromNotFound("id")));
        when(mockContact.hasNullProperties(anyBoolean())).thenReturn(false);
        when(mockContact.getOwnerBase64Id()).thenReturn(IdUtils.toBase64(0L));
        when(mockContact.getContactBase64Id()).thenReturn(IdUtils.toBase64(1L));
        doReturn(false).when(dao).isPersisted(mockContact);

        ApiResult<Contact> res = dao.insert(mockContact);
        res.consume(__ -> fail(), e -> {
        });
    }

    @Test
    void insert_atomic() throws SQLException {
        Boolean prevAutoCommit = true;
        User mockUser = mock(User.class);
        Contact mock = mock(Contact.class);
        UserDAO userDAO = mock(UserDAO.class);
        ContactDAO dao = spy(new ContactDAO(connection, userDAO));

        when(userDAO.byId(anyLong())).thenReturn(ApiResult.ok(mockUser));
        when(connection.getAutoCommit()).thenReturn(prevAutoCommit);
        when(mock.hasNullProperties(anyBoolean())).thenReturn(false);
        when(mock.getOwnerBase64Id()).thenReturn(IdUtils.toBase64(0L));
        when(mock.getContactBase64Id()).thenReturn(IdUtils.toBase64(1L));
        doReturn(false).when(dao).isPersisted(mock);
        when(statement.executeUpdate()).thenThrow(SQLException.class);

        ApiResult<Contact> res = dao.insert(mock);
        res.consume(__ -> fail(), e -> assertEquals(500, e.statusCode()));
        verify(connection).setAutoCommit(prevAutoCommit);
        verify(connection).setAutoCommit(false);
        verify(connection).rollback();
    }

    @Test
    void ofUser_withInvalidBase64() {
        ContactDAO.withNewObjects(connection).ofUser("asd")
                .consume(__ -> fail(), __ -> {
                });
    }

    @Test
    void ofUser_withValidBase64() throws SQLException {
        String id = IdUtils.toBase64(0L);
        when(results.next()).then(new Answer<Boolean>() {
            private boolean answered = false;

            @Override
            public Boolean answer(InvocationOnMock invocationOnMock) {
                if (!answered) {
                    answered = true;
                    return true;
                }
                return false;
            }
        });
        when(results.getLong(1)).thenReturn(0L);
        when(results.getLong(2)).thenReturn(1L);
        ContactDAO.withNewObjects(connection)
                .ofUser(id)
                .consume(cs -> {
                    assertNotNull(cs);
                    assertEquals(1, cs.size());
                    assertEquals(IdUtils.toBase64(0L), cs.get(0).getOwnerBase64Id());
                    assertEquals(IdUtils.toBase64(1L), cs.get(0).getContactBase64Id());
                }, e -> fail());
    }
}