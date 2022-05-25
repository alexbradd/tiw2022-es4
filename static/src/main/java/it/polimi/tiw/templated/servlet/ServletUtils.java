package it.polimi.tiw.templated.servlet;

import it.polimi.tiw.api.beans.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

class ServletUtils {
    public static User tryExtractFromSession(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) {
            return null;
        }
        return (User) session.getAttribute("user");
    }
}
