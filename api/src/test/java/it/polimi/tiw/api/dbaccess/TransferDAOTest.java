package it.polimi.tiw.api.dbaccess;

import it.polimi.tiw.api.beans.Account;
import it.polimi.tiw.api.beans.Transfer;
import it.polimi.tiw.api.error.ApiError;
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

import java.sql.*;
import java.time.Instant;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferDAOTest {
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
        assertThrows(NullPointerException.class, () -> new TransferDAO(null, null));
        assertThrows(NullPointerException.class, () -> new TransferDAO(connection, null));
        TransferDAO.withNewObjects(connection).insert(null).consume(a -> fail(), e -> {
        });
        TransferDAO.withNewObjects(connection).update(null).consume(a -> fail(), e -> {
        });
        TransferDAO.withNewObjects(connection).byId(null).consume(a -> fail(), e -> {
        });
        TransferDAO.withNewObjects(connection).inAndOutOf(null).consume(a -> fail(), e -> {
        });
        TransferDAO.withNewObjects(connection).newTransfer(null, null, 0, "a").consume(a -> fail(), e -> {
        });
        TransferDAO.withNewObjects(connection).newTransfer(IdUtils.toBase64(0L), null, 0, "a").consume(a -> fail(), e -> {
        });
        TransferDAO.withNewObjects(connection).newTransfer(IdUtils.toBase64(0L), IdUtils.toBase64(1L), 0, null).consume(a -> fail(), e -> {
        });
        assertFalse(TransferDAO.withNewObjects(connection).isPersisted(null));
    }

    @Test
    void byId_invalidBase64() {
        TransferDAO.withNewObjects(connection).byId("asdfa").consume(a -> fail(), e -> {
        });
    }

    @Test
    void byId_notInDb() throws SQLException {
        when(results.next()).thenReturn(false);
        ApiResult<Transfer> res = TransferDAO.withNewObjects(connection).byId("AAAAAAAAAAA");
        verify(statement).executeQuery();
        verify(results).next();
        assertTrue(res.match((Transfer a) -> false, (ApiError e) -> true));
    }

    @Test
    void byId_inDb() throws SQLException {
        when(results.next()).thenReturn(true);
        when(results.getLong(anyString())).thenReturn(0L);
        when(results.getDouble(anyString())).thenReturn(100.0);
        when(results.getTimestamp(anyString())).thenReturn(Timestamp.from(Instant.now()));
        ApiResult<Transfer> res = TransferDAO.withNewObjects(connection).byId("AAAAAAAAAAA");
        assertTrue(res.match((Transfer a) -> true, (ApiError e) -> false));
    }

    @ParameterizedTest
    @MethodSource("isPersisted_invalidTransferSource")
    void isPersisted_invalidTransfer(Transfer invalid) throws SQLException {
        Mockito.lenient().when(results.next()).thenReturn(false);
        assertFalse(TransferDAO.withNewObjects(connection).isPersisted(invalid));
    }

    static Stream<Transfer> isPersisted_invalidTransferSource() {
        Transfer invalidId = mock(Transfer.class, "invalidId");
        Transfer notInDb = mock(Transfer.class, "notInDb");

        when(notInDb.getBase64Id()).thenReturn(IdUtils.toBase64(0L));
        return Stream.of(invalidId, notInDb);
    }

    @Test
    void isPersisted_inDb() throws SQLException {
        Transfer a = mock(Transfer.class);
        TransferDAO dao = TransferDAO.withNewObjects(connection);
        when(a.getBase64Id()).thenReturn(IdUtils.toBase64(0L));
        when(results.next()).thenReturn(true);
        when(results.getLong(anyString())).thenReturn(0L);
        when(results.getDouble(anyString())).thenReturn(100.0);
        when(results.getTimestamp(anyString())).thenReturn(Timestamp.from(Instant.now()));
        assertTrue(dao.isPersisted(a));
    }

    @Test
    void update_errors() {
        Transfer t = mock(Transfer.class);
        TransferDAO.withNewObjects(connection)
                .update(t)
                .consume(__ -> fail(), __ -> {
                });
    }

    @ParameterizedTest
    @MethodSource("insert_invalidTransferSource")
    void insert_withInvalidTransfer(Transfer invalid) {
        TransferDAO dao = TransferDAO.withNewObjects(connection);
        dao.insert(invalid).consume(__ -> fail(), __ -> {
        });
    }

    static Stream<Transfer> insert_invalidTransferSource() {
        Transfer withNull = mock(Transfer.class, "withNull");
        Transfer withInvalidId = mock(Transfer.class, "withInvalidId");
        Transfer withInvalidToId = mock(Transfer.class, "withInvalidToId");
        Transfer withInvalidFromId = mock(Transfer.class, "withInvalidFromId");
        Transfer withInvalidAmount = mock(Transfer.class, "withInvalidAmount");

        when(withNull.hasNullProperties(anyBoolean())).thenReturn(true);

        when(withInvalidToId.getBase64Id()).thenReturn(IdUtils.toBase64(0L));
        when(withInvalidToId.getFromId()).thenReturn(IdUtils.toBase64(0L));
        when(withInvalidToId.getAmount()).thenReturn(1.0);

        when(withInvalidFromId.getBase64Id()).thenReturn(IdUtils.toBase64(0L));
        when(withInvalidFromId.getToId()).thenReturn(IdUtils.toBase64(0L));
        when(withInvalidFromId.getAmount()).thenReturn(1.0);

        when(withInvalidAmount.getBase64Id()).thenReturn(IdUtils.toBase64(0L));
        when(withInvalidAmount.getFromId()).thenReturn(IdUtils.toBase64(0L));
        when(withInvalidAmount.getToId()).thenReturn(IdUtils.toBase64(0L));
        when(withInvalidAmount.getAmount()).thenReturn(-1.0);

        return Stream.of(withNull, withInvalidId, withInvalidToId, withInvalidFromId, withInvalidAmount);
    }

    @Test
    void insert_withAlreadyPersistedTransfer() {
        Transfer mock = mock(Transfer.class);
        TransferDAO dao = spy(TransferDAO.withNewObjects(connection));

        when(mock.hasNullProperties(anyBoolean())).thenReturn(false);
        when(mock.getBase64Id()).thenReturn(IdUtils.toBase64(0L));
        when(mock.getFromId()).thenReturn(IdUtils.toBase64(0L));
        when(mock.getToId()).thenReturn(IdUtils.toBase64(0L));
        when(mock.getAmount()).thenReturn(1.0);

        doReturn(true).when(dao).isPersisted(mock);
        dao.insert(mock).consume(__ -> fail(), __ -> {
        });
    }

    @Test
    void insert_atomic() throws SQLException {
        Boolean prevAutoCommit = true;
        Transfer mock = mock(Transfer.class);
        TransferDAO dao = spy(TransferDAO.withNewObjects(connection));

        when(connection.getAutoCommit()).thenReturn(prevAutoCommit);
        when(mock.hasNullProperties(anyBoolean())).thenReturn(false);
        when(mock.getBase64Id()).thenReturn(IdUtils.toBase64(0L));
        when(mock.getDate()).thenReturn(Instant.now());
        when(mock.getFromId()).thenReturn(IdUtils.toBase64(0L));
        when(mock.getToId()).thenReturn(IdUtils.toBase64(0L));
        when(mock.getAmount()).thenReturn(1.0);
        doReturn(false).when(dao).isPersisted(mock);
        when(statement.executeUpdate()).thenThrow(SQLException.class);

        ApiResult<Transfer> res = dao.insert(mock);
        res.consume(__ -> fail(), e -> assertEquals(500, e.statusCode()));
        verify(connection).setAutoCommit(prevAutoCommit);
        verify(connection).setAutoCommit(false);
        verify(connection).rollback();
    }

    @Test
    void inAndOutOf_withInvalidBase64() {
        TransferDAO.withNewObjects(connection).inAndOutOf("asd")
                .consume(__ -> fail(), __ -> {
                });
    }

    @Test
    void inAndOutOf_withValidId() throws SQLException {
        // first to, second from
        int n = 2;
        String id = IdUtils.toBase64(1L);
        when(results.next()).thenAnswer(new Answer<Boolean>() {
            private int counter = n;

            @Override
            public Boolean answer(InvocationOnMock invocationOnMock) {
                try {
                    return counter > 0;
                } finally {
                    counter--;
                }
            }
        });
        when(results.getLong(anyString())).thenReturn(100L);
        when(results.getLong("toId")).thenAnswer(new Answer<Long>() {
            private int counter = n;

            @Override
            public Long answer(InvocationOnMock invocationOnMock) {
                try {
                    if (counter % 2 == 0)
                        return 1L;
                    return 0L;
                } finally {
                    counter--;
                }
            }
        });
        when(results.getLong("fromId")).thenAnswer(new Answer<Long>() {
            private int counter = n;

            @Override
            public Long answer(InvocationOnMock invocationOnMock) {
                try {
                    if (counter % 2 != 0)
                        return 1L;
                    return 0L;
                } finally {
                    counter--;
                }
            }
        });
        when(results.getDouble(anyString())).thenReturn(100.0);
        when(results.getDouble("amount")).thenAnswer(new Answer<Double>() {
            private int counter = n;

            @Override
            public Double answer(InvocationOnMock invocationOnMock) {
                try {
                    if (counter % 2 == 0)
                        return 100.0;
                    return 200.0;
                } finally {
                    counter--;
                }
            }
        });
        when(results.getTimestamp(anyString())).thenReturn(Timestamp.from(Instant.now()));
        TransferDAO.withNewObjects(connection)
                .inAndOutOf(id)
                .consume(t -> {
                    assertNotNull(t);
                    assertEquals(1, t.getFirst().size());
                    assertEquals(1, t.getSecond().size());
                    assertEquals(100, t.getFirst().get(0).getAmount());
                    assertEquals(200, t.getSecond().get(0).getAmount());
                }, e -> fail());
    }

    @ParameterizedTest
    @MethodSource("newTransfer_invalidParameterSource")
    void newTransfer_withInvalidParameters(NewTransferParameters params) {
        TransferDAO.withNewObjects(connection)
                .newTransfer(params.fromId, params.toId, params.amount, params.causal)
                .consume(__ -> fail(), __ -> {
                });
    }

    static Stream<NewTransferParameters> newTransfer_invalidParameterSource() {
        return Stream.of(
                new NewTransferParameters("asdf", IdUtils.toBase64(0L), 1, "a"),
                new NewTransferParameters(IdUtils.toBase64(0L), "asdf", 1, "a"),
                new NewTransferParameters(IdUtils.toBase64(0L), IdUtils.toBase64(1L), 0, "a"),
                new NewTransferParameters(IdUtils.toBase64(0L), IdUtils.toBase64(1L), -1, "a"),
                new NewTransferParameters(IdUtils.toBase64(0L), IdUtils.toBase64(0L), 1, "a"),
                new NewTransferParameters(IdUtils.toBase64(0L), IdUtils.toBase64(1L), 1, "")
        );
    }

    private static class NewTransferParameters {
        public String fromId;
        public String toId;
        public int amount;
        public String causal;

        public NewTransferParameters(String fromId, String toId, int amount, String causal) {
            this.fromId = fromId;
            this.toId = toId;
            this.amount = amount;
            this.causal = causal;
        }
    }

    @Test
    void newTransfer_withAccountsNotInDatabase() throws SQLException {
        AccountDAO mock = mock(AccountDAO.class);
        TransferDAO dao = new TransferDAO(connection, mock);

        when(connection.getAutoCommit()).thenReturn(true);
        when(mock.byId(anyString())).thenReturn(ApiResult.error(new ApiError(404, "")));

        dao.newTransfer(IdUtils.toBase64(0L), IdUtils.toBase64(1L), 1, "a")
                .consume(__ -> fail(), __ -> {
                });
        verify(connection).rollback();
    }

    @Test
    void newTransfer_withNotEnoughBalance() throws SQLException {
        Account mockAccount = mock(Account.class);
        AccountDAO mockDao = mock(AccountDAO.class);
        TransferDAO dao = new TransferDAO(connection, mockDao);

        when(connection.getAutoCommit()).thenReturn(true);
        when(mockDao.byId(anyString())).thenReturn(ApiResult.ok(mockAccount));
        when(mockAccount.getBalance()).thenReturn(0.0);

        dao.newTransfer(IdUtils.toBase64(0L), IdUtils.toBase64(1L), 1, "a")
                .consume(__ -> fail(), __ -> {
                });
        verify(connection).rollback();
    }

    @Test
    void newTransfer_failedUpdate() throws SQLException {
        Account mockAccount = mock(Account.class);
        AccountDAO mockDao = mock(AccountDAO.class);
        TransferDAO dao = new TransferDAO(connection, mockDao);

        when(connection.getAutoCommit()).thenReturn(true);
        when(mockDao.update(any(Account.class))).thenReturn(ApiResult.error(new ApiError(500, "")));
        when(mockDao.byId(anyString())).thenReturn(ApiResult.ok(mockAccount));
        when(mockAccount.getBalance()).thenReturn(10.0);

        dao.newTransfer(IdUtils.toBase64(0L), IdUtils.toBase64(1L), 1, "a")
                .consume(__ -> fail(), __ -> {
                });
        verify(connection).rollback();
    }

    @Test
    void newTransfer_failedInsert() throws SQLException {
        Account mockAccount = mock(Account.class);
        AccountDAO mockDao = mock(AccountDAO.class);
        TransferDAO dao = spy(new TransferDAO(connection, mockDao));

        when(connection.getAutoCommit()).thenReturn(true);
        when(mockDao.update(any(Account.class))).thenReturn(ApiResult.ok(mockAccount));
        when(mockDao.byId(anyString())).thenReturn(ApiResult.ok(mockAccount));
        when(mockAccount.getBalance()).thenReturn(10.0);
        doReturn(ApiResult.error(new ApiError(500, ""))).when(dao).insert(any(Transfer.class));

        dao.newTransfer(IdUtils.toBase64(0L), IdUtils.toBase64(1L), 1, "a")
                .consume(__ -> fail(), __ -> {
                });
        verify(connection).rollback();
    }

    @Test
    void newTransfer_noRollbackIfNotInManualTransactionHandling() throws SQLException {
        Account mockAccount = mock(Account.class);
        AccountDAO mockDao = mock(AccountDAO.class);
        TransferDAO dao = spy(new TransferDAO(connection, mockDao));

        when(connection.getAutoCommit()).thenReturn(false);
        when(mockDao.update(any(Account.class))).thenReturn(ApiResult.ok(mockAccount));
        when(mockDao.byId(anyString())).thenReturn(ApiResult.ok(mockAccount));
        when(mockAccount.getBalance()).thenReturn(10.0);
        doReturn(ApiResult.error(new ApiError(500, ""))).when(dao).insert(any(Transfer.class));

        dao.newTransfer(IdUtils.toBase64(0L), IdUtils.toBase64(1L), 1, "a")
                .consume(__ -> fail(), __ -> {
                });
        verify(connection, never()).rollback();
    }
}