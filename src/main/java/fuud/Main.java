package fuud;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.XmlWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws Exception {
        final XmlWebApplicationContext mvcContext = new XmlWebApplicationContext() {
            @Override
            protected Resource getResourceByPath(String path) {
                return new FileSystemResource(path);
            }
        };
        mvcContext.setConfigLocation("classpath:application-context.xml");
        mvcContext.refresh();

        ResourceHandler resourceHandler = createResourceHandler();
        ServletContextHandler servletHandler = getServletContextHandler(mvcContext);

        HandlerList handlerList = new HandlerList();
        handlerList.setHandlers(new Handler[]{resourceHandler, servletHandler});

        final int port = 8080;

        Server server = new Server(port);
        server.setHandler(handlerList);

        server.start();
        server.join();
    }

    private static ResourceHandler createResourceHandler() {
        ResourceHandler handler = new ResourceHandler();
        handler.setDirectoriesListed(true);
        handler.setWelcomeFiles(new String[]{"index.html"});
        handler.setBaseResource(org.eclipse.jetty.util.resource.Resource.newClassPathResource("/WEB-INF/static/"));
        return handler;
    }

    private static ServletContextHandler getServletContextHandler(XmlWebApplicationContext mvcContext) throws IOException {

        ServletContextHandler contextHandler = new ServletContextHandler();
        contextHandler.setErrorHandler(null);
        contextHandler.setContextPath("/");

        // register spring-mvc servlet
        contextHandler.addServlet(new ServletHolder(new DispatcherServlet(mvcContext)), "/*");
        contextHandler.addEventListener(new ContextLoaderListener(mvcContext));

        return contextHandler;
    }
}
