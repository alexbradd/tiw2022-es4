package it.polimi.tiw.api.beans;

import java.time.Instant;

/**
 * Bean representing a money transfer between two {@link Account}.
 */
public class Transfer implements PersistedObject {
    /**
     * The maximum length of the causal message
     */
    public static final int CAUSAL_LENGTH = 1024;

    private String base64Id;
    private Instant date;
    private int amount;
    private String toId;
    private int toBalance;
    private String fromId;
    private int fromBalance;
    private String causal;

    /**
     * Getter for this Transfer's id. The returned id might be null, e.g. when the transfer has not yet been saved to
     * database
     *
     * @return this Transfer's id
     */
    public String getBase64Id() {
        return base64Id;
    }

    /**
     * Sets the id for this Transfer.
     *
     * @param base64Id the new Transfer ID.
     */
    public void setBase64Id(String base64Id) {
        this.base64Id = base64Id;
    }

    /**
     * Getter for the date of this Transfer
     *
     * @return the date of this Transfer
     */
    public Instant getDate() {
        return date;
    }

    /**
     * Sets the date of this Transfer
     *
     * @param date the new date
     */
    public void setDate(Instant date) {
        this.date = date;
    }

    /**
     * Getter for the amount of money transferred with this Transfer.
     *
     * @return the amount of money transferred with this Transfer
     */
    public int getAmount() {
        return amount;
    }

    /**
     * Sets the amount of money transferred by this Transfer
     *
     * @param amount the new amount
     */
    public void setAmount(int amount) {
        this.amount = amount;
    }

    /**
     * Getter for the id of the {@link Account} to which the money went.
     *
     * @return the {@link Account} to which the money went.
     */
    public String getToId() {
        return toId;
    }

    /**
     * Sets the id of  the {@link Account} to which the money went.
     *
     * @param toId the new {@link Account} to which the money went.
     */
    public void setToId(String toId) {
        this.toId = toId;
    }

    /**
     * Getter for the balance that the destination {@link Account} had previous to this transfer.
     *
     * @return the balance that the destination {@link Account} had previous to this transfer.
     */
    public int getToBalance() {
        return toBalance;
    }

    /**
     * Sets the balance that the destination {@link Account} had previous to this transfer.
     *
     * @param toBalance the new balance that the destination {@link Account} had previous to this transfer.
     */
    public void setToBalance(int toBalance) {
        this.toBalance = toBalance;
    }

    /**
     * Getter for the id of the {@link Account} from which the money came.
     *
     * @return the {@link Account} from which the money came.
     */
    public String getFromId() {
        return fromId;
    }

    /**
     * Sets the {@link Account} from which the money came.
     *
     * @param fromId the new {@link Account} from which the money came
     */
    public void setFromId(String fromId) {
        this.fromId = fromId;
    }

    /**
     * Getter for the balance that the source {@link Account} had previous to this transfer.
     *
     * @return the balance that the source {@link Account} had previous to this transfer.
     */
    public int getFromBalance() {
        return fromBalance;
    }

    /**
     * Sets the balance that the source {@link Account} had previous to this transfer.
     *
     * @param fromBalance the new balance that the source {@link Account} had previous to this transfer.
     */
    public void setFromBalance(int fromBalance) {
        this.fromBalance = fromBalance;
    }

    /**
     * Getter for the causal message associated with this Transfer
     *
     * @return the causal message associated with this Transfer
     */
    public String getCausal() {
        return causal;
    }

    /**
     * Sets the causal message associated with this Transfer
     *
     * @param causal the new causal message associated with this Transfer
     */
    public void setCausal(String causal) {
        this.causal = causal;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasNullProperties(boolean includeId) {
        return (includeId && base64Id == null) ||
                date == null ||
                toId == null ||
                fromId == null ||
                causal == null;
    }
}
