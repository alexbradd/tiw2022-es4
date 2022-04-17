package it.polimi.tiw.api.dbaccess;

import it.polimi.tiw.api.exceptions.UpdateException;
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
import java.util.Base64;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TestUser {
    @Mock
    private ConnectionRetriever mockConnectionRetriever;
    @Mock
    private Connection mockConnection;
    @Mock
    private PreparedStatement statement;
    @Mock
    private ResultSet results;

    @BeforeEach
    void setupMocks() throws SQLException {
        User.retriever = mockConnectionRetriever;
        Mockito.lenient().when(mockConnectionRetriever.getConnection()).thenReturn(mockConnection);
        Mockito.lenient().when(mockConnection.prepareStatement(any(String.class))).thenReturn(statement);
        Mockito.lenient().when(statement.executeUpdate()).thenReturn(1);
        Mockito.lenient().when(statement.executeQuery()).thenReturn(results);
    }

    @Test
    void bondCheck() {
        User user = new User("Pippo", "pippoTheBest", "pippo@email.com", "Pippo", "Pluto");
        assertThrows(NullPointerException.class, () -> new User(null, null, null, null, null));
        assertThrows(IllegalArgumentException.class, () -> new User("a".repeat(200), "a".repeat(200), "a".repeat(200), "a".repeat(200), "a".repeat(200)));
        assertThrows(NullPointerException.class, () -> user.setUsername(null));
        assertThrows(IllegalArgumentException.class, () -> user.setUsername("a".repeat(200)));
        assertThrows(NullPointerException.class, () -> user.setSaltedPassword(null));
        assertThrows(NullPointerException.class, () -> user.setEmail(null));
        assertThrows(IllegalArgumentException.class, () -> user.setEmail("a".repeat(200)));
        assertThrows(IllegalArgumentException.class, () -> user.setEmail("not an email"));
        assertThrows(NullPointerException.class, () -> user.setName(null));
        assertThrows(IllegalArgumentException.class, () -> user.setName("a".repeat(200)));
        assertThrows(NullPointerException.class, () -> user.setSurname(null));
        assertThrows(IllegalArgumentException.class, () -> user.setSurname("a".repeat(200)));
    }

    @Test
    void testSavingNewUser() throws SQLException {
        when(results.next()).thenReturn(true);
        when(results.getLong(any(String.class))).thenReturn(0L);
        User user = new User("Pippo", "pippoTheBest", "pippo@email.com", "Pippo", "Pluto");
        user.save();
        verify(statement).executeUpdate();
        verify(results).getLong(any(String.class));
        assertTrue(user.getId().isPresent());
        assertEquals(
                Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[]{0, 0, 0, 0, 0, 0, 0, 0}),
                user.getId().get());
    }

    @Test
    void testSavingDuplicate() throws SQLException {
        when(statement.executeUpdate()).thenThrow(SQLException.class);
        User user = new User("Pippo", "pippoTheBest", "pippo@email.com", "Pippo", "Pluto");
        assertThrows(UpdateException.class, user::save);
    }

    @Test
    void testUpdatingExistingUser() throws SQLException {
        User user = spy(
                new User("Pippo", "pippoTheBest", "pippo@email.com", "Pippo", "Pluto")
        );
        when(user.isPersisted()).thenReturn(true);
        when(user.getLongId()).thenReturn(Optional.of(0L));
        user.save();
        verify(statement).executeUpdate();
    }

    @Test
    void testFindByUsernameNotInDb() throws SQLException {
        when(results.next()).thenReturn(false);
        Optional<User> maybe = User.byUsername("pippo");
        verify(statement).executeQuery();
        verify(results).next();
        assertTrue(maybe.isEmpty());
    }

    @Test
    void testFindByUsernameInDb() throws SQLException {
        when(results.next()).thenReturn(true);
        when(results.getLong(any(String.class))).thenReturn(0L);
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
        Optional<User> maybe = User.byUsername("pippo");
        verify(statement).executeQuery();
        verify(results).next();
        assertTrue(maybe.isPresent());
        User user = maybe.get();
        assertTrue(user.getId().isPresent());
        assertEquals(
                Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[]{0, 0, 0, 0, 0, 0, 0, 0}),
                user.getId().get());
        assertEquals("pippo", user.getUsername());
        assertEquals("pippo@email.com", user.getEmail());
        assertEquals("Pippo", user.getName());
        assertEquals("Pluto", user.getSurname());
    }

    @Test
    void testFindByIdNotInDb() throws SQLException {
        when(results.next()).thenReturn(false);
        Optional<User> maybe = User.byId("AAAAAAAAAAA");
        verify(statement).executeQuery();
        verify(results).next();
        assertTrue(maybe.isEmpty());
    }

    @Test
    void testFindByIdInDb() throws SQLException {
        when(results.next()).thenReturn(true);
        when(results.getLong(any(String.class))).thenReturn(0L);
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
        Optional<User> maybe = User.byId("AAAAAAAAAAA");
        verify(statement).executeQuery();
        verify(results).next();
        assertTrue(maybe.isPresent());
        User user = maybe.get();
        assertTrue(user.getId().isPresent());
        assertEquals(
                Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[]{0, 0, 0, 0, 0, 0, 0, 0}),
                user.getId().get());
        assertEquals("pippo", user.getUsername());
        assertEquals("pippo@email.com", user.getEmail());
        assertEquals("Pippo", user.getName());
        assertEquals("Pluto", user.getSurname());
    }
}
