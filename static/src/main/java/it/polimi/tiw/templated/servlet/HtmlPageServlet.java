package it.polimi.tiw.templated.servlet;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;

import javax.servlet.ServletContext;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;

/**
 * This servlet tries to retrieve an HTML template, process it using Thymeleaf and send it. If it fails to find a
 * template, it falls back to sending simple html pages. If it cannot find anything, it sends a 404.
 * <p>
 * If any parameters are passed in, they are piped into the template context (if a parameter is present multiple times,
 * only the first is considered).
 */
@WebServlet("*.html")
public class HtmlPageServlet extends HttpServlet {
    private TemplateEngine templateEngine;

    /**
     * {@inheritDoc}
     */
    public void init() {
        ServletContext context = getServletContext();
        ServletContextTemplateResolver resolver = new ServletContextTemplateResolver(context);
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setPrefix("/WEB-INF/templates/");
        resolver.setSuffix(".html");

        this.templateEngine = new TemplateEngine();
        this.templateEngine.setTemplateResolver(resolver);
    }

    /**
     * {@inheritDoc}
     */
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("text/html;charset=UTF-8");
        res.setHeader("Cache-Control", "no-cache");
        res.setDateHeader("Expires", 0);

        if (isTemplated(req))
            templatePage(req, res);
        else if (isStaticPage(req))
            streamStaticPage(req, res);
        else
            res.sendError(404);
    }

    /**
     * true <==> req is to a templated page
     */
    private boolean isTemplated(HttpServletRequest req) throws MalformedURLException {
        ServletContext context = getServletContext();
        return context.getResource("/WEB-INF/templates" + req.getServletPath()) != null;
    }

    /**
     * true <==> req is to a non templated page
     */
    private boolean isStaticPage(HttpServletRequest req) throws MalformedURLException {
        ServletContext context = getServletContext();
        return context.getResource(req.getServletPath()) != null;
    }

    /**
     * runs thymeleaf on the template
     */
    private void templatePage(HttpServletRequest req, HttpServletResponse res) throws IOException {
        ServletContext context = getServletContext();
        WebContext ctx = new WebContext(req, res, context, req.getLocale());

        HttpSession s = req.getSession(false);
        if (s != null)
            ctx.setVariable("user", s.getAttribute("user"));

        templateEngine.process(req.getServletPath(), ctx, res.getWriter());
    }

    /**
     * streams from disk the page the old way
     */
    private void streamStaticPage(HttpServletRequest req, HttpServletResponse res) throws IOException {
        ServletContext context = getServletContext();
        char[] buf = new char[1024];
        PrintWriter out = res.getWriter();

        try (InputStream is = context.getResourceAsStream(req.getServletPath())) {
            try (InputStreamReader i = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                while (i.read(buf) > 0)
                    out.write(buf);
            }
        }
    }
}