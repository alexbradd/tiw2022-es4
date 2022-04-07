package it.polimi.tiw;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.messageresolver.IMessageResolver;
import org.thymeleaf.messageresolver.StandardMessageResolver;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;

import java.io.*;
import javax.servlet.ServletContext;
import javax.servlet.http.*;
import javax.servlet.annotation.*;

@WebServlet("*.html")
public class PlainHtmlPage extends HttpServlet {
    TemplateEngine templateEngine;

    public void init() {
        ServletContext context = getServletContext();
        ServletContextTemplateResolver resolver = new ServletContextTemplateResolver(context);
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setPrefix("/WEB-INF/templates");
        resolver.setSuffix("*.html");

        this.templateEngine = new TemplateEngine();
        this.templateEngine.setTemplateResolver(resolver);
    }

    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        ServletContext context = getServletContext();
        WebContext ctx = new WebContext(req, res, context, req.getLocale());

        res.setContentType("text/html;charset=UTF-8");
        res.setHeader("Pragma", "no-cache");
        res.setHeader("Cache-Control", "no-cache");
        res.setDateHeader("Expires", 0);

        templateEngine.process(req.getServletPath(), ctx, res.getWriter());
    }

    public void destroy() {
    }
}