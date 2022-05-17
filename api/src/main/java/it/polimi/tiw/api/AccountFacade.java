package it.polimi.tiw.api;

import it.polimi.tiw.api.beans.Account;
import it.polimi.tiw.api.beans.User;
import it.polimi.tiw.api.dbaccess.AccountDAO;
import it.polimi.tiw.api.dbaccess.ConnectionRetriever;
import it.polimi.tiw.api.dbaccess.ProductionConnectionRetriever;
import it.polimi.tiw.api.dbaccess.UserDAO;
import it.polimi.tiw.api.functional.ApiResult;

import java.sql.Connection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Class exposing a simple interface for manipulating accounts.
 */
public class AccountFacade {
    private final ConnectionRetriever retriever;
    private final Function<Connection, UserDAO> userDAOGenerator;
    private final Function<Connection, AccountDAO> accountDAOGenerator;

    /**
     * Creates a new AccountFacade with the specified objects.
     *
     * @param retriever           the {@link ConnectionRetriever} to use
     * @param userDAOGenerator    a {@link Supplier} of {@link UserDAO}
     * @param accountDAOGenerator a {@link Supplier} of {@link AccountDAO}
     * @throws NullPointerException if any parameter is null
     */
    public AccountFacade(ConnectionRetriever retriever, Function<Connection, UserDAO> userDAOGenerator, Function<Connection, AccountDAO> accountDAOGenerator) {
        this.retriever = Objects.requireNonNull(retriever);
        this.userDAOGenerator = Objects.requireNonNull(userDAOGenerator);
        this.accountDAOGenerator = Objects.requireNonNull(accountDAOGenerator);
    }

    /**
     * Create a new {@link Account} with 0 balance for {@link User} with the specified id
     *
     * @param id the id of owner of the new {@link Account}
     * @return an {@link ApiResult} containing the created {@link Account} if everything went ok
     * @throws NullPointerException if {@code id} is null
     * @see AccountDAO#insert(Account)
     */
    public ApiResult<Account> createFor(String id) {
        Objects.requireNonNull(id);
        return retriever.with(c ->
                userDAOGenerator.apply(c)
                        .byId(id)
                        .flatMap(u -> {
                            Account a = new Account(u.getBase64Id(), 0);
                            return accountDAOGenerator.apply(c).insert(a);
                        }));
    }

    /**
     * Returns a list of all the {@link Account} of a given {@link User}
     *
     * @param u the owner of the {@link Account} in the list
     * @return an {@link ApiResult} containing a {@link List} of {@link Account} if everything went ok
     * @throws NullPointerException if {@code u} is null
     * @see AccountDAO#ofUser(String)
     */
    public ApiResult<List<Account>> ofUser(User u) {
        Objects.requireNonNull(u);
        return retriever.with(c ->
                accountDAOGenerator.apply(c).ofUser(u.getBase64Id()));
    }

    /**
     * Creates a new AccountFacade using the default objects
     *
     * @return a new AccountFacade
     */
    public static AccountFacade withDefaultObjects() {
        return new AccountFacade(ProductionConnectionRetriever.getInstance(), UserDAO::new, AccountDAO::new);
    }
}
