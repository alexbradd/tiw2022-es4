package it.polimi.tiw.api.dbaccess;

import it.polimi.tiw.api.ApiError;
import it.polimi.tiw.api.ApiResult;
import it.polimi.tiw.api.beans.User;
import it.polimi.tiw.api.utils.IdUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserDAOTest {
    @Mock
    private Connection mockConnection;
    @Mock
    private PreparedStatement statement;
    @Mock
    private ResultSet results;

    @BeforeEach
    void setupMocks() throws SQLException {
        Mockito.lenient().when(mockConnection.prepareStatement(any(String.class))).thenReturn(statement);
        Mockito.lenient().when(statement.executeUpdate()).thenReturn(1);
        Mockito.lenient().when(statement.executeQuery()).thenReturn(results);
    }

    @Test
    void all_withNull() {
        assertThrows(NullPointerException.class, () -> new UserDAO(null));
        new UserDAO(mockConnection).insert(null).consume(__ -> fail(), __ -> {
        });
        new UserDAO(mockConnection).update(null).consume(__ -> fail(), __ -> {
        });
        new UserDAO(mockConnection).byId(null).consume(__ -> fail(), __ -> {
        });
        new UserDAO(mockConnection).byUsername(null).consume(__ -> fail(), __ -> {
        });
        assertFalse(new UserDAO(mockConnection).isPersisted(null));
    }

    @Test
    void byId_withKnownLongId() throws SQLException {
        when(results.next()).thenReturn(true);
        when(results.getString(anyString())).thenAnswer(invocation -> {
            String column = invocation.getArgument(0);
            return switch (column) {
                case "username" -> "pippo";
                case "password" -> "AA:AA";
                case "email" -> "email@email.com";
                case "name" -> "Pippo";
                case "surname" -> "Pluto";
                default -> fail();
            };
        });
        when(results.getLong("id")).thenReturn(0L);

        ApiResult<User> res = new UserDAO(mockConnection).byId(0L);
        res.consume(u -> {
            assertEquals(IdUtils.toBase64(0L), u.getBase64Id());
            assertEquals("pippo", u.getUsername());
            assertEquals("email@email.com", u.getEmail());
            assertEquals("AA:AA", u.getSaltedPassword());
            assertEquals("Pippo", u.getName());
            assertEquals("Pluto", u.getSurname());
        }, __ -> fail());
    }

    @Test
    void byId_withUnknownLongId() throws SQLException {
        when(results.next()).thenReturn(false);

        ApiResult<User> res = new UserDAO(mockConnection).byId(0L);
        res.consume(u -> fail(), e -> assertEquals(404, e.statusCode()));
    }

    @Test
    void byId_withKnownBase64() throws SQLException {
        when(results.next()).thenReturn(true);
        when(results.getString(anyString())).thenAnswer(invocation -> {
            String column = invocation.getArgument(0);
            return switch (column) {
                case "username" -> "pippo";
                case "password" -> "AA:AA";
                case "email" -> "email@email.com";
                case "name" -> "Pippo";
                case "surname" -> "Pluto";
                default -> fail();
            };
        });
        when(results.getLong("id")).thenReturn(0L);

        ApiResult<User> res = new UserDAO(mockConnection).byId("AAAAAAAAAAA");
        res.consume(u -> {
            assertEquals(IdUtils.toBase64(0L), u.getBase64Id());
            assertEquals("pippo", u.getUsername());
            assertEquals("email@email.com", u.getEmail());
            assertEquals("AA:AA", u.getSaltedPassword());
            assertEquals("Pippo", u.getName());
            assertEquals("Pluto", u.getSurname());
        }, __ -> fail());
    }

    @Test
    void byId_withInvalidBase64Id() {
        ApiResult<User> res = new UserDAO(mockConnection).byId("asdf");
        res.consume(__ -> fail(), __ -> {
        });
    }

    @Test
    void byId_withUnknownBase64() throws SQLException {
        when(results.next()).thenReturn(false);

        ApiResult<User> res = new UserDAO(mockConnection).byId("AAAAAAAAAAA");
        res.consume(u -> fail(), e -> assertEquals(404, e.statusCode()));
    }

    @Test
    void byUsername_withUnknownUsername() throws SQLException {
        when(results.next()).thenReturn(false);

        ApiResult<User> res = new UserDAO(mockConnection).byUsername("aa");
        res.consume(u -> fail(), e -> assertEquals(404, e.statusCode()));
    }

    @Test
    void byUsername_withKnownUsername() throws SQLException {
        when(results.next()).thenReturn(true);
        when(results.getString(anyString())).thenAnswer(invocation -> {
            String column = invocation.getArgument(0);
            return switch (column) {
                case "username" -> "pippo";
                case "password" -> "AA:AA";
                case "email" -> "email@email.com";
                case "name" -> "Pippo";
                case "surname" -> "Pluto";
                default -> fail();
            };
        });
        when(results.getLong("id")).thenReturn(0L);

        ApiResult<User> res = new UserDAO(mockConnection).byUsername("pippo");
        res.consume(u -> {
            assertEquals(IdUtils.toBase64(0L), u.getBase64Id());
            assertEquals("pippo", u.getUsername());
            assertEquals("email@email.com", u.getEmail());
            assertEquals("AA:AA", u.getSaltedPassword());
            assertEquals("Pippo", u.getName());
            assertEquals("Pluto", u.getSurname());
        }, __ -> fail());
    }

    @Test
    void isPersisted_withUserWithoutId() {
        User mock = mock(User.class);
        when(mock.getBase64Id()).thenReturn(null);

        assertFalse(new UserDAO(mockConnection).isPersisted(mock));
    }

    @Test
    void isPersisted_withUserWithIdNotInDb() throws SQLException {
        User mock = mock(User.class);
        when(mock.getBase64Id()).thenReturn("AAAAAAAAAAA");
        when(results.next()).thenReturn(false);

        assertFalse(new UserDAO(mockConnection).isPersisted(mock));
    }

    @Test
    void isPersisted_withUserInDb() throws SQLException {
        User mock = mock(User.class);
        when(mock.getBase64Id()).thenReturn(IdUtils.toBase64(0L));
        when(results.next()).thenReturn(true);
        when(results.getString(anyString())).thenAnswer(invocation -> {
            String column = invocation.getArgument(0);
            return switch (column) {
                case "username" -> "pippo";
                case "password" -> "AA:AA";
                case "email" -> "email@email.com";
                case "name" -> "Pippo";
                case "surname" -> "Pluto";
                default -> fail();
            };
        });
        when(results.getLong("id")).thenReturn(0L);

        assertTrue(new UserDAO(mockConnection).isPersisted(mock));
    }

    @Test
    void update_withNotPersistedUser() {
        User mock = mock(User.class);
        UserDAO dao = spy(new UserDAO(mockConnection));

        when(mock.hasNullProperties(anyBoolean())).thenReturn(false);
        Mockito.lenient().doReturn(false).when(dao).isPersisted(any(User.class));

        dao.update(mock).consume(__ -> fail(), __ -> {
        });
    }

    @Test
    void update_insert_withUserWithNullProperties() {
        User mock = mock(User.class);
        UserDAO dao = spy(new UserDAO(mockConnection));

        when(mock.hasNullProperties(anyBoolean())).thenReturn(true);

        dao.update(mock).consume(__ -> fail(), __ -> {
        });
        dao.insert(mock).consume(__ -> fail(), __ -> {
        });
    }

    @Test
    void update_withInvalidBase64() {
        User mock = mock(User.class);
        UserDAO dao = spy(new UserDAO(mockConnection));

        when(mock.hasNullProperties(anyBoolean())).thenReturn(false);
        when(mock.getBase64Id()).thenReturn("asdf");

        dao.update(mock).consume(__ -> fail(), __ -> {
        });
    }

    @Test
    void update_atomic() throws SQLException {
        Boolean prevAutoCommit = true;
        User mock = mock(User.class);
        UserDAO dao = spy(new UserDAO(mockConnection));

        when(mockConnection.getAutoCommit()).thenReturn(prevAutoCommit);
        when(mock.hasNullProperties(anyBoolean())).thenReturn(false);
        when(mock.getBase64Id()).thenReturn(IdUtils.toBase64(0L));
        doReturn(true).when(dao).isPersisted(mock);
        when(statement.executeUpdate()).thenThrow(SQLException.class);

        ApiResult<User> res = dao.update(mock);
        res.consume(__ -> fail(), e -> assertEquals(500, e.statusCode()));
        verify(mockConnection).setAutoCommit(prevAutoCommit);
        verify(mockConnection).setAutoCommit(false);
        verify(mockConnection).rollback();
    }

    @Test
    void insert_withAlreadyPresentUsername() {
        User mock = mock(User.class);
        UserDAO dao = spy(new UserDAO(mockConnection));

        when(mock.hasNullProperties(false)).thenReturn(false);
        when(mock.getUsername()).thenReturn("pippo");
        doReturn(ApiResult.ok(mock)).when(dao).byUsername("pippo");
        Mockito.lenient().doReturn(false).when(dao).isPersisted(mock);

        dao.insert(mock).consume(__ -> fail(), e -> assertEquals(409, e.statusCode()));
    }

    @Test
    void insert_withAlreadyPersistedUser() {
        User mock = mock(User.class);
        UserDAO dao = spy(new UserDAO(mockConnection));

        when(mock.hasNullProperties(false)).thenReturn(false);
        doReturn(true).when(dao).isPersisted(mock);

        dao.insert(mock).consume(__ -> fail(), e -> assertEquals(400, e.statusCode()));
    }

    @Test
    void insert_atomic() throws SQLException {
        Boolean prevAutoCommit = true;
        User mock = mock(User.class);
        UserDAO dao = spy(new UserDAO(mockConnection));

        when(mockConnection.getAutoCommit()).thenReturn(prevAutoCommit);
        when(mock.hasNullProperties(anyBoolean())).thenReturn(false);
        when(mock.getUsername()).thenReturn("pippo");
        doReturn(false).when(dao).isPersisted(mock);
        doReturn(ApiResult.error(new ApiError(404, ""))).when(dao).byUsername("pippo");
        when(statement.executeUpdate()).thenThrow(SQLException.class);

        ApiResult<User> res = dao.insert(mock);
        res.consume(__ -> fail(), e -> assertEquals(500, e.statusCode()));
        verify(mockConnection).setAutoCommit(prevAutoCommit);
        verify(mockConnection).setAutoCommit(false);
        verify(mockConnection).rollback();
    }
}
