package it.polimi.tiw.templated.servlet;

import it.polimi.tiw.api.beans.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

class ServletUtils {
    public static <T> T tryExtractFromSession(HttpServletRequest req, String name, Class<T> clazz) {
        HttpSession session = req.getSession(false);
        if (session == null) {
            return null;
        }
        return clazz.cast(session.getAttribute(name));
    }
}
