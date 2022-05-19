package it.polimi.tiw.templated;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Convenience wrapper around a generic list of Strings that represents the navigation history of the user.
 */
public class History {
    private final ArrayList<String> history;

    /**
     * Creates a new empty History
     */
    public History() {
        history = new ArrayList<>();
    }

    /**
     * Pushes a new url into the History
     *
     * @param url the url to add
     * @throws NullPointerException if {@code url} is null
     */
    public void push(String url) {
        Objects.requireNonNull(url);
        history.add(0, url);
    }

    /**
     * Pops the last url from the History. If the history is empty nothing is done
     */
    public void pop() {
        if (!history.isEmpty()) history.remove(0);
    }

    /**
     * Returns the url of the page currently viewed by the user. If it cannot be retrieved, returns null
     *
     * @return the url of the page currently viewed by the user or null
     */
    public String current() {
        if (history.isEmpty())
            return null;
        return history.get(0);
    }

    /**
     * Returns the last visited URL from the history, assuming that the last added URL is the page the user is currently
     * on. This call does not modify the history.
     *
     * @return the last viewed URL or null if the history is not big enough
     */
    public String last() {
        if (!hasLast())
            return null;
        return history.get(1);
    }

    /**
     * Returns true if there is enough history to know the page currently viewed by the user.
     *
     * @return true if there is enough history to know the page currently viewed by the user
     */
    public boolean hasCurrent() {
        return history.size() >= 1;
    }

    /**
     * Returns true if the history has enough items to return a valid url after calling {@link #last()}.
     *
     * @return true if the history has a last viewed page.
     */
    public boolean hasLast() {
        return history.size() >= 2;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "History{" +
                "history=" + history +
                '}';
    }
}
