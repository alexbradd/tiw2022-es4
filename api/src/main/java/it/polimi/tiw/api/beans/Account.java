package it.polimi.tiw.api.beans;

import java.util.Objects;

/**
 * Bean representing an Account.
 */
public class Account implements PersistedObject {
    private String base64Id;
    private String ownerId;
    private int balance;

    /**
     * Creates a new Account without an id, belonging to the given {@link User} and with the specified balance
     *
     * @param ownerId the {@link User} that own this account
     * @param balance the balance of the account
     * @throws NullPointerException     if {@code user} is null
     * @throws IllegalArgumentException if {@code balance} is less than zero
     */
    public Account(String ownerId, int balance) {
        setOwnerId(ownerId);
        setBalance(balance);
    }

    /**
     * Creates a new Account the specified id, belonging to the given {@link User} and with the specified balance
     *
     * @param base64Id the id of this account encoded in url safe Base64
     * @param ownerId  the {@link User} that own this account
     * @param balance  the balance of the account
     * @throws NullPointerException     if {@code user} is null
     * @throws IllegalArgumentException if {@code balance} is less than zero
     */
    public Account(String base64Id, String ownerId, int balance) {
        this(ownerId, balance);
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
     * Returns the id of the {@link User} that owns this Account
     *
     * @return the id of the {@link User} that owns this Account
     */
    public String getOwnerId() {
        return ownerId;
    }

    /**
     * Sets the id of the new  {@link User} that owns this Account
     *
     * @param ownerId the new Account owner
     * @throws NullPointerException if {@code ownerId} is null
     */
    public void setOwnerId(String ownerId) {
        this.ownerId = Objects.requireNonNull(ownerId);
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
        return (base64Id == null && includeId) || ownerId == null;
    }
}
