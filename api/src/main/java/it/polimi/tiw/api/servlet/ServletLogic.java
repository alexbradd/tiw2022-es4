package it.polimi.tiw.api.servlet;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * Because checked exceptions. Duplicate of {@link Runnable} but with {@link ServletException} and {@link IOException}
 * tacked onto it. Other business logic are not required since they are unchecked.
 */
@FunctionalInterface
interface ServletLogic {
    /**
     * @throws ServletException thrown when a servlet encounters difficulty
     * @throws IOException      thrown if an error occurs during IO
     * @see Runnable#run()
     */
    void run() throws ServletException, IOException;
}
