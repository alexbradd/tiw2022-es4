package it.polimi.tiw.api;

import it.polimi.tiw.api.beans.Account;
import it.polimi.tiw.api.beans.User;
import it.polimi.tiw.api.dbaccess.AccountDAO;
import it.polimi.tiw.api.dbaccess.ProductionConnectionRetriever;
import it.polimi.tiw.api.dbaccess.UserDAO;

import java.util.List;
import java.util.Objects;

/**
 * Container for all {@link AccountDAO} related calls
 */
public class AccountApi {
    /**
     * Class is static
     */
    private AccountApi() {
    }

    /**
     * Create a new {@link Account} with 0 balance for {@link User} with the specified id
     *
     * @param id the id of owner of the new {@link Account}
     * @return an {@link ApiResult} containing the created {@link Account} if everything went ok
     * @throws NullPointerException if {@code id} is null
     * @see AccountDAO#insert(Account)
     */
    public static ApiResult<Account> createFor(String id) {
        Objects.requireNonNull(id);
        return ProductionConnectionRetriever.getInstance().with(c ->
                new UserDAO(c).byId(id)
                        .flatMap(u -> {
                            Account a = new Account(u, 0);
                            return AccountDAO.withNewObjects(c).insert(a);
                        }));
    }

    /**
     * Returns a list of all the {@link Account} of a given {@link User}
     *
     * @param u the owner of the {@link Account} in the list
     * @return an {@link ApiResult} containing a {@link List} of {@link Account} if everything went ok
     * @throws NullPointerException if {@code u} is null
     * @see AccountDAO#ofUser(User)
     */
    public static ApiResult<List<Account>> ofUser(User u) {
        Objects.requireNonNull(u);
        return ProductionConnectionRetriever.getInstance().with(c ->
                AccountDAO.withNewObjects(c).ofUser(u));
    }
}
