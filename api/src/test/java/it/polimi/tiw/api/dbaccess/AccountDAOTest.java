package it.polimi.tiw.api.dbaccess;

import it.polimi.tiw.api.beans.Account;
import it.polimi.tiw.api.error.ApiError;
import it.polimi.tiw.api.functional.ApiResult;
import it.polimi.tiw.api.utils.IdUtils;
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

    @BeforeEach
    void setupMocks() throws SQLException {
        Mockito.lenient().when(mockConnection.prepareStatement(any(String.class))).thenReturn(statement);
        Mockito.lenient().when(statement.executeUpdate()).thenReturn(1);
        Mockito.lenient().when(statement.executeQuery()).thenReturn(results);
    }

    @Test
    void all_withNull() {
        assertThrows(NullPointerException.class, () -> new AccountDAO(null));
        new AccountDAO(mockConnection).insert(null).consume(a -> fail(), e -> {
        });
        new AccountDAO(mockConnection).update(null).consume(a -> fail(), e -> {
        });
        new AccountDAO(mockConnection).byId(null).consume(a -> fail(), e -> {
        });
        new AccountDAO(mockConnection).ofUser(null).consume(a -> fail(), e -> {
        });
        assertFalse(new AccountDAO(mockConnection).isPersisted(null));
    }

    @Test
    void byId_invalidBase64() {
        new AccountDAO(mockConnection).byId("asdfa").consume(a -> fail(), e -> {
        });
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
        when(results.getLong("ownerId")).thenReturn(0L);
        when(results.getDouble("balance")).thenReturn(100.0);
        ApiResult<Account> res = new AccountDAO(mockConnection).byId("AAAAAAAAAAA");
        assertTrue(res.match((Account a) -> true, (ApiError e) -> false));
    }

    @Test
    void isPersisted_notInDbNullId() throws SQLException {
        Account a = mock(Account.class);
        Mockito.lenient().when(a.getBase64Id()).thenReturn(null);
        Mockito.lenient().when(results.next()).thenReturn(false);
        assertFalse(new AccountDAO(mockConnection).isPersisted(a));
    }

    @Test
    void isPersisted_notInDbValidId() throws SQLException {
        Account a = mock(Account.class);
        when(a.getBase64Id()).thenReturn(IdUtils.toBase64(0L));
        when(results.next()).thenReturn(false);
        assertFalse(new AccountDAO(mockConnection).isPersisted(a));
    }

    @Test
    void isPersisted_inDb() throws SQLException {
        Account a = mock(Account.class);
        AccountDAO dao = spy(new AccountDAO(mockConnection));
        when(a.getBase64Id()).thenReturn(IdUtils.toBase64(0L));
        when(results.next()).thenReturn(true);
        when(results.getLong("ownerId")).thenReturn(0L);
        when(results.getDouble("balance")).thenReturn(100.0);
        assertTrue(dao.isPersisted(a));
    }

    @Test
    void update_withNotPersistedAccount() {
        Account mock = mock(Account.class);
        AccountDAO dao = spy(new AccountDAO(mockConnection));

        when(mock.hasNullProperties(anyBoolean())).thenReturn(false);
        Mockito.lenient().doReturn(false).when(dao).isPersisted(any(Account.class));

        dao.update(mock).consume(__ -> fail(), __ -> {
        });
    }

    @Test
    void update_insert_withAccountWithNullProperties() {
        Account mock = mock(Account.class);
        AccountDAO dao = spy(new AccountDAO(mockConnection));

        when(mock.hasNullProperties(anyBoolean())).thenReturn(true);

        dao.update(mock).consume(__ -> fail(), __ -> {
        });
        dao.insert(mock).consume(__ -> fail(), __ -> {
        });
    }

    @Test
    void update_withInvalidBase64() {
        Account mock = mock(Account.class);
        AccountDAO dao = spy(new AccountDAO(mockConnection));

        Mockito.lenient().when(mock.hasNullProperties(anyBoolean())).thenReturn(false);
        Mockito.lenient().when(mock.getBase64Id()).thenReturn("asdf");

        dao.update(mock).consume(__ -> fail(), __ -> {
        });
    }

    @Test
    void update_atomic() throws SQLException {
        Boolean prevAutoCommit = true;
        Account mock = mock(Account.class);
        AccountDAO dao = spy(new AccountDAO(mockConnection));

        when(mockConnection.getAutoCommit()).thenReturn(prevAutoCommit);
        when(mock.hasNullProperties(anyBoolean())).thenReturn(false);
        when(mock.getBase64Id()).thenReturn(IdUtils.toBase64(0L));
        when(mock.getOwnerId()).thenReturn(IdUtils.toBase64(0L));
        doReturn(true).when(dao).isPersisted(mock);
        when(statement.executeUpdate()).thenThrow(SQLException.class);

        ApiResult<Account> res = dao.update(mock);
        res.consume(__ -> fail(), e -> assertEquals(500, e.statusCode()));
        verify(mockConnection).setAutoCommit(prevAutoCommit);
        verify(mockConnection).setAutoCommit(false);
        verify(mockConnection).rollback();
    }

    @Test
    void insert_withAlreadyPersistedAccount() {
        Account mock = mock(Account.class);
        AccountDAO dao = spy(new AccountDAO(mockConnection));

        Mockito.lenient().when(mock.hasNullProperties(false)).thenReturn(false);
        Mockito.lenient().doReturn(true).when(dao).isPersisted(mock);

        dao.insert(mock).consume(__ -> fail(), e -> assertEquals(400, e.statusCode()));
    }

    @Test
    void insert_atomic() throws SQLException {
        Boolean prevAutoCommit = true;
        Account mock = mock(Account.class);
        AccountDAO dao = spy(new AccountDAO(mockConnection));

        when(mockConnection.getAutoCommit()).thenReturn(prevAutoCommit);
        when(mock.hasNullProperties(anyBoolean())).thenReturn(false);
        when(mock.getOwnerId()).thenReturn(IdUtils.toBase64(0L));
        doReturn(false).when(dao).isPersisted(mock);
        when(statement.executeUpdate()).thenThrow(SQLException.class);

        ApiResult<Account> res = dao.insert(mock);
        res.consume(__ -> fail(), e -> assertEquals(500, e.statusCode()));
        verify(mockConnection).setAutoCommit(prevAutoCommit);
        verify(mockConnection).setAutoCommit(false);
        verify(mockConnection).rollback();
    }

    @Test
    void ofUser_unsavedUser() {
        new AccountDAO(mockConnection).ofUser(null).consume(a -> fail(), e -> {
        });
    }

    @Test
    void ofUser_invalidUser() {
        new AccountDAO(mockConnection).ofUser("sium").consume(a -> fail(), e -> {
        });
    }

    @Test
    void ofUser_noAccounts() throws SQLException {
        when(results.next()).thenReturn(false);
        ApiResult<List<Account>> res = new AccountDAO(mockConnection).ofUser(IdUtils.toBase64(0L));
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
        ApiResult<List<Account>> res = new AccountDAO(mockConnection).ofUser(IdUtils.toBase64(0L));
        assertTrue(res.match((List<Account> a) -> true, (ApiError e) -> false));
        res.consume(l -> assertEquals(1, l.size()), e -> fail());
        verify(statement).executeQuery();
        verify(results, times(2)).next();
    }
}