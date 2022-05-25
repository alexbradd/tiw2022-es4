package it.polimi.tiw.templated.servlet;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.extras.java8time.dialect.Java8TimeDialect;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;

import javax.servlet.ServletContext;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * This servlet tries to retrieve an HTML template, process it using Thymeleaf and send it.
 */
@WebServlet(value = {
        "/index.html",
        "/details.html",
        "/register.html",
        "/login.html",
        "/rejectTransfer.html",
        "/confirmTransfer.html"
})
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
        this.templateEngine.addDialect(new Java8TimeDialect());
        this.templateEngine.setTemplateResolver(resolver);
    }

    /**
     * {@inheritDoc}
     */
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("text/html;charset=UTF-8");
        res.setHeader("Cache-Control", "no-cache");
        res.setDateHeader("Expires", 0);

        templatePage(req, res);
    }

    /**
     * runs thymeleaf on the template
     */
    private void templatePage(HttpServletRequest req, HttpServletResponse res) throws IOException {
        ServletContext context = getServletContext();
        WebContext ctx = new WebContext(req, res, context, req.getLocale());

        templateEngine.process(req.getServletPath(), ctx, res.getWriter());
    }
}