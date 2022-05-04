package it.polimi.tiw.api.dbaccess;

import it.polimi.tiw.api.ApiError;
import it.polimi.tiw.api.ApiResult;
import it.polimi.tiw.api.beans.Account;
import it.polimi.tiw.api.beans.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountDAOTest {
    @Mock
    private Connection mockConnection;
    @Mock
    private PreparedStatement statement;
    @Mock
    private ResultSet results;

    @Mock
    private Account a;
    @Mock
    private User u;

    @BeforeEach
    void setupMocks() throws SQLException {
        Mockito.lenient().when(mockConnection.prepareStatement(any(String.class))).thenReturn(statement);
        Mockito.lenient().when(statement.executeUpdate()).thenReturn(1);
        Mockito.lenient().when(statement.executeQuery()).thenReturn(results);

        Mockito.lenient().when(a.getOwner()).thenReturn(u);
        Mockito.lenient().when(a.getBalance()).thenReturn(100);
        Mockito.lenient().when(a.getBase64Id()).thenReturn("AAAAAAAAAAA");

        Mockito.lenient().when(u.getBase64Id()).thenReturn("AAAAAAAAAAA");
        Mockito.lenient().when(u.getUsername()).thenReturn("pippo");
        Mockito.lenient().when(u.getSaltedPassword()).thenReturn("AAAAA:AAAA");
        Mockito.lenient().when(u.getEmail()).thenReturn("pippo@email.com");
        Mockito.lenient().when(u.getName()).thenReturn("Pippo");
        Mockito.lenient().when(u.getSurname()).thenReturn("Pluto");
    }

    @Test
    void nullCheck() {
        assertThrows(NullPointerException.class, () -> new AccountDAO(null));
        assertThrows(NullPointerException.class, () -> new AccountDAO(mockConnection).save(null));
        assertThrows(NullPointerException.class, () -> new AccountDAO(mockConnection).isPersisted(null));
        assertThrows(NullPointerException.class, () -> new AccountDAO(mockConnection).byId(null));
        assertThrows(NullPointerException.class, () -> new AccountDAO(mockConnection).ofUser(null));
    }

    @Test
    void byId_invalidId() {
        assertThrows(IllegalArgumentException.class, () -> new AccountDAO(mockConnection).byId("asdfa"));
    }

    @Test
    void byId_notInDb() throws SQLException {
        when(results.next()).thenReturn(false);
        ApiResult<Account> res = new AccountDAO(mockConnection).byId("AAAAAAAAAAA");
        verify(statement).executeQuery();
        verify(results).next();
        assertTrue(res.match((Account a) -> false, (ApiError e) -> true));
    }

    @Test
    void byId_inDb() throws SQLException {
        when(results.next()).thenReturn(true);
        when(results.getString(any(String.class))).thenAnswer((Answer<String>) invocationCall -> {
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
        when(results.getLong("id")).thenReturn(0L);
        when(results.getLong("ownerId")).thenReturn(0L);
        when(results.getInt("balance")).thenReturn(100);
        ApiResult<Account> res = new AccountDAO(mockConnection).byId("AAAAAAAAAAA");
        assertTrue(res.match((Account a) -> true, (ApiError e) -> false));
    }

    @Test
    void persisted_notInDb() throws SQLException {
        when(results.next()).thenReturn(false);
        assertFalse(new AccountDAO(mockConnection).isPersisted(a));
    }

    @Test
    void persisted_inDb() {
        AccountDAO dao = spy(new AccountDAO(mockConnection));
        doReturn(ApiResult.ok(a)).when(dao).byId(any(String.class));
        assertTrue(dao.isPersisted(a));
    }

    @Test
    void save_newAccount() throws SQLException {
        AccountDAO dao = spy(new AccountDAO(mockConnection));
        doReturn(false).when(dao).isPersisted(any(Account.class));
        ApiResult<Account> res = dao.save(a);
        assertTrue(res.match((Account a) -> true, (ApiError e) -> false));
        res.consume(a -> assertEquals("AAAAAAAAAAA", a.getBase64Id()), e -> fail());
        verify(statement).executeUpdate();
    }

    @Test
    void save_invalidId() {
        Account account = mock(Account.class);
        AccountDAO dao = spy(new AccountDAO(mockConnection));
        doReturn(true).when(dao).isPersisted(any(Account.class));
        when(account.getOwner()).thenReturn(u);
        when(account.getBase64Id()).thenReturn("asdfad");
        assertThrows(IllegalArgumentException.class, () -> dao.save(account));
    }

    @Test
    void save_existingAccount() throws SQLException {
        AccountDAO dao = spy(new AccountDAO(mockConnection));
        doReturn(true).when(dao).isPersisted(any(Account.class));
        ApiResult<Account> res = dao.save(a);
        assertTrue(res.match((Account a) -> true, (ApiError e) -> false));
        verify(statement).executeUpdate();
    }

    @Test
    void ofUser_unsavedUser() {
        User u = mock(User.class);
        when(u.getBase64Id()).thenReturn(null);
        assertThrows(NullPointerException.class, () -> new AccountDAO(mockConnection).ofUser(u));
    }

    @Test
    void ofUser_invalidUser() {
        User u = mock(User.class);
        when(u.getBase64Id()).thenReturn("asdf");
        assertThrows(IllegalArgumentException.class, () -> new AccountDAO(mockConnection).ofUser(u));
    }

    @Test
    void ofUser_noAccounts() throws SQLException {
        when(results.next()).thenReturn(false);
        ApiResult<List<Account>> res = new AccountDAO(mockConnection).ofUser(u);
        assertTrue(res.match((List<Account> a) -> true, (ApiError e) -> false));
        res.consume(l -> assertTrue(l.isEmpty()), e -> fail());
        verify(statement).executeQuery();
        verify(results).next();
    }

    @Test
    void ofUser_accounts() throws SQLException {
        when(results.next()).thenAnswer(new Answer<>() {
            private boolean returned = false;

            @Override
            public Boolean answer(InvocationOnMock invocationOnMock) {
                boolean toRet = !returned;
                returned = true;
                return toRet;
            }
        });
        ApiResult<List<Account>> res = new AccountDAO(mockConnection).ofUser(u);
        assertTrue(res.match((List<Account> a) -> true, (ApiError e) -> false));
        res.consume(l -> assertEquals(1, l.size()), e -> fail());
        verify(statement).executeQuery();
        verify(results, times(2)).next();
    }
}