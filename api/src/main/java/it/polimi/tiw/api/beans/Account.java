package it.polimi.tiw.api.beans;

import java.util.Objects;

/**
 * Bea representing an Account.
 */
public class Account implements PersistedObject {
    private String base64Id;
    private User owner;
    private int balance;

    /**
     * Creates a new Account without an id, belonging to the given {@link User} and with the specified balance
     *
     * @param owner   the {@link User} that own this account
     * @param balance the balance of the account
     * @throws NullPointerException     if {@code user} is null
     * @throws IllegalArgumentException if {@code balance} is less than zero
     */
    public Account(User owner, int balance) {
        setOwner(owner);
        setBalance(balance);
    }

    /**
     * Creates a new Account the specified id, belonging to the given {@link User} and with the specified balance
     *
     * @param base64Id the id of this account encoded in url safe Base64
     * @param owner    the {@link User} that own this account
     * @param balance  the balance of the account
     * @throws NullPointerException     if {@code user} is null
     * @throws IllegalArgumentException if {@code balance} is less than zero
     */
    public Account(String base64Id, User owner, int balance) {
        this(owner, balance);
        setBase64Id(base64Id);
    }

    /**
     * Getter for this Account's id. The returned id might be null, e.g. when the account has not yet been saved to
     * database
     *
     * @return this Account's id
     */
    public String getBase64Id() {
        return base64Id;
    }

    /**
     * Sets the id for this Account.
     *
     * @param base64Id the new Account ID.
     */
    public void setBase64Id(String base64Id) {
        this.base64Id = base64Id;
    }

    /**
     * Returns the {@link User} that owns this Account
     *
     * @return the {@link User} that owns this Account
     */
    public User getOwner() {
        return owner;
    }

    /**
     * Sets a new {@link User} as owner of this Account
     *
     * @param owner the new Account owner
     * @throws NullPointerException if {@code owner} is null
     */
    public void setOwner(User owner) {
        this.owner = Objects.requireNonNull(owner);
    }

    /**
     * Returns the balance of this Account.
     *
     * @return the balance of this Account.
     */
    public int getBalance() {
        return balance;
    }

    /**
     * Sets the current balance of this account
     *
     * @param balance the new balance
     * @throws IllegalArgumentException if {@code balance} is less than 0
     */
    public void setBalance(int balance) {
        if (balance < 0)
            throw new IllegalArgumentException("balance should be positive");
        this.balance = balance;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasNullProperties(boolean includeId) {
        return (base64Id == null && includeId) || owner == null;
    }
}
