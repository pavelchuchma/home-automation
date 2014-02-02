package servlet;

import app.NodeInfoCollector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


public class Servlet  extends AbstractHandler
{
    private NodeInfoCollector nodeInfoCollector;

    public Servlet(NodeInfoCollector nodeInfoCollector) {
        this.nodeInfoCollector = nodeInfoCollector;
    }

    public void handle(String target,
                       Request baseRequest,
                       HttpServletRequest request,
                       HttpServletResponse response)
            throws IOException, ServletException
    {
        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);
        response.getWriter().println(nodeInfoCollector.getReport());
    }

    public static void startServer(NodeInfoCollector nodeInfoCollector) throws Exception
    {
        Server server = new Server(80);
        server.setHandler(new Servlet(nodeInfoCollector));

        server.start();
        server.join();
    }
}