package it.polimi.tiw.templated.filters;

import it.polimi.tiw.templated.History;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

/**
 * Tracks user movement through the website. It uses a {@link History} saved into the current session as {@code history}.
 * If the request has the {@code ret} parameter, a page will be popped from the stack. If a session is not present, it
 * will be created with containing an empty stack.
 */
public class HistoryFilter extends HttpFilter {
    /**
     * {@inheritDoc}
     */
    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpSession session = req.getSession();
        History history = Objects.requireNonNullElseGet(
                (History) session.getAttribute("history"),
                History::new);
        String reqUrl = reconstructUrl(req);
        String ret = req.getParameter("ret");

        if (!reqUrl.equals(history.current())) {
            if (ret == null) {
                history.push(reqUrl);
                session.setAttribute("history", history);
                chain.doFilter(req, res);
            } else {
                history.pop();
                session.setAttribute("history", history);

                String newReqUrl = reconstructUrl(req, queryStringWihtoutRet(req));
                res.sendRedirect(newReqUrl);
            }
            return;
        }
        chain.doFilter(req, res);
    }

    /**
     * Reconstructs url as {@code /.../...?[querystring]}
     */
    private String reconstructUrl(HttpServletRequest req) {
        return reconstructUrl(req, req.getQueryString());
    }

    /**
     * Reconstructs url as {@code /.../...?[qs]} with qs given as param
     */
    private String reconstructUrl(HttpServletRequest req, String qs) {
        return req.getRequestURI() + (qs == null || qs.length() == 0
                ? ""
                : "?" + qs);
    }

    /**
     * Removes {@code ret} parameter from the querystring
     */
    private String queryStringWihtoutRet(HttpServletRequest req) {
        Map<String, String[]> params = req.getParameterMap();
        return params.entrySet().stream()
                .filter(e -> !e.getKey().equals("ret"))
                .collect(StringBuilder::new,
                        (acc, e) -> {
                            for (String v : e.getValue())
                                acc.append(e.getKey())
                                        .append("=")
                                        .append(v)
                                        .append("&");
                        },
                        StringBuilder::append)
                .toString();
    }
}
