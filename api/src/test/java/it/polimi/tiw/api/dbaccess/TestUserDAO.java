package it.polimi.tiw.api.dbaccess;

import it.polimi.tiw.api.ApiError;
import it.polimi.tiw.api.ApiResult;
import it.polimi.tiw.api.beans.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TestUserDAO {
    @Mock
    private Connection mockConnection;
    @Mock
    private PreparedStatement statement;
    @Mock
    private ResultSet results;

    @Mock
    private User u;

    @BeforeEach
    void setupMocks() throws SQLException {
        Mockito.lenient().when(mockConnection.prepareStatement(any(String.class))).thenReturn(statement);
        Mockito.lenient().when(statement.executeUpdate()).thenReturn(1);
        Mockito.lenient().when(statement.executeQuery()).thenReturn(results);
        Mockito.lenient().when(u.getBase64Id()).thenReturn("AAAAAAAAAAA");
        Mockito.lenient().when(u.getUsername()).thenReturn("pippo");
        Mockito.lenient().when(u.getSaltedPassword()).thenReturn("AAAAA:AAAA");
        Mockito.lenient().when(u.getEmail()).thenReturn("pippo@email.com");
        Mockito.lenient().when(u.getName()).thenReturn("Pippo");
        Mockito.lenient().when(u.getSurname()).thenReturn("Pluto");
    }

    @Test
    void bondCheck() {
        assertThrows(NullPointerException.class, () -> new UserDAO(null));
    }

    @Test
    void testSavingNewUser() throws SQLException {
        when(results.next()).thenReturn(true);
        UserDAO user = new UserDAO(mockConnection);
        ApiResult<User> r = user.save(u);
        verify(statement).executeUpdate();
        assertTrue(r.match((User u) -> true, (ApiError e) -> false));
    }

    @Test
    void testSavingDuplicate() throws SQLException {
        when(statement.executeUpdate()).thenThrow(SQLException.class);
        UserDAO user = new UserDAO(mockConnection);
        assertTrue(user.save(u).match((User u) -> false, (ApiError e) -> true));
    }

    @Test
    void testUpdatingExistingUser() throws SQLException {
        UserDAO user = spy(new UserDAO(mockConnection));
        doReturn(ApiResult.ok(u)).when(user).byUsername(any(String.class));
        ApiResult<User> r = user.save(u);
        verify(statement).executeUpdate();
        assertTrue(r.match((User u) -> true, (ApiError e) -> false));
    }

    @Test
    void testFindByUsernameNotInDb() throws SQLException {
        when(results.next()).thenReturn(false);
        ApiResult<User> maybe = new UserDAO(mockConnection).byUsername("pippo");
        verify(statement).executeQuery();
        verify(results).next();
        assertTrue(maybe.match((User u) -> false, (ApiError e) -> true));
    }

    @Test
    void testFindByUsernameInDb() throws SQLException {
        when(results.next()).thenReturn(true);
        when(results.getString(any(String.class))).then((Answer<String>) invocationCall -> {
            String key = invocationCall.getArgument(0);
            return switch (key) {
                case "username" -> "pippo";
                case "password" -> "pippoTheBest";
                case "email" -> "pippo@email.com";
                case "name" -> "Pippo";
                case "surname" -> "Pluto";
                default -> null;
            };
        });
        when(results.getLong(any(String.class))).thenReturn(0L);
        ApiResult<User> maybe = new UserDAO(mockConnection).byUsername("pippo");
        verify(statement).executeQuery();
        verify(results).next();
        assertTrue(maybe.match((User u) -> true, (ApiError e) -> false));
        maybe.consume(
                (User user) -> {
                    assertEquals("AAAAAAAAAAA", user.getBase64Id());
                    assertEquals("pippo", user.getUsername());
                    assertEquals("pippo@email.com", user.getEmail());
                    assertEquals("Pippo", user.getName());
                    assertEquals("Pluto", user.getSurname());
                },
                e -> fail()
        );
    }

    @Test
    void testFindByIdNotInDb() throws SQLException {
        when(results.next()).thenReturn(false);
        ApiResult<User> maybe = new UserDAO(mockConnection).byId(0);
        verify(statement).executeQuery();
        verify(results).next();
        assertTrue(maybe.match((User u) -> false, (ApiError e) -> true));
    }

    @Test
    void testFindByIdInDb() throws SQLException {
        when(results.next()).thenReturn(true);
        when(results.getString(any(String.class))).then((Answer<String>) invocationCall -> {
            String key = invocationCall.getArgument(0);
            return switch (key) {
                case "username" -> "pippo";
                case "password" -> "pippoTheBest";
                case "email" -> "pippo@email.com";
                case "name" -> "Pippo";
                case "surname" -> "Pluto";
                default -> null;
            };
        });
        when(results.getLong(any(String.class))).thenReturn(0L);
        ApiResult<User> maybe = new UserDAO(mockConnection).byId(0);
        verify(statement).executeQuery();
        verify(results).next();
        assertTrue(maybe.match((User u) -> true, (ApiError e) -> false));
        maybe.consume(
                (User user) -> {
                    assertEquals("AAAAAAAAAAA", user.getBase64Id());
                    assertEquals("pippo", user.getUsername());
                    assertEquals("pippo@email.com", user.getEmail());
                    assertEquals("Pippo", user.getName());
                    assertEquals("Pluto", user.getSurname());
                },
                e -> fail()
        );
    }
}
