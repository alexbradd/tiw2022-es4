package it.polimi.tiw.api.beans;

/**
 * Bean representing a request for transferring money from an account to another
 */
public class NewTransferRequest {
    private String fromUserId;
    private String fromAccountId;
    private String toUserId;
    private String toAccountId;
    private double amount;
    private String causal;

    /**
     * Returns the source Account's owner id
     *
     * @return the source Account's owner id
     */
    public String getFromUserId() {
        return fromUserId;
    }

    /**
     * Sets the source Account's owner id
     *
     * @param fromUserId the source Account's owner id
     */
    public void setFromUserId(String fromUserId) {
        this.fromUserId = fromUserId;
    }

    /**
     * Returns the source Account id
     *
     * @return the source Account id
     */
    public String getFromAccountId() {
        return fromAccountId;
    }

    /**
     * Sets the source Account id
     *
     * @param fromAccountId the source Account id
     */
    public void setFromAccountId(String fromAccountId) {
        this.fromAccountId = fromAccountId;
    }

    /**
     * Returns the destination Account's owner id
     *
     * @return the destination Account's owner id
     */
    public String getToUserId() {
        return toUserId;
    }

    /**
     * Sets the destination Account's owner id
     *
     * @param toUserId the destination Account's owner id
     */
    public void setToUserId(String toUserId) {
        this.toUserId = toUserId;
    }

    /**
     * Returns the destination Account id
     *
     * @return the destination Account id
     */
    public String getToAccountId() {
        return toAccountId;
    }

    /**
     * Sets the source Account id
     *
     * @param toAccountId the source Account id
     */
    public void setToAccountId(String toAccountId) {
        this.toAccountId = toAccountId;
    }

    /**
     * Returns the amount of money to be transferred
     *
     * @return the amount of money to be transferred
     */
    public double getAmount() {
        return amount;
    }

    /**
     * Sets the amount of money to be transferred
     *
     * @param amount the amount of money to be transferred
     */
    public void setAmount(double amount) {
        this.amount = amount;
    }

    /**
     * Returns the causal message
     *
     * @return the causal message
     */
    public String getCausal() {
        return causal;
    }

    /**
     * Sets the causal message
     *
     * @param causal the causal message
     */
    public void setCausal(String causal) {
        this.causal = causal;
    }
}
