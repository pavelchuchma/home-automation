package servlet;

import app.NodeInfoCollector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;


public class Servlet extends AbstractHandler {
    private NodeInfoCollector nodeInfoCollector;

    public Servlet(NodeInfoCollector nodeInfoCollector) {
        this.nodeInfoCollector = nodeInfoCollector;
    }

    public void handle(String target,
                       Request baseRequest,
                       HttpServletRequest request,
                       HttpServletResponse response)
            throws IOException, ServletException {
        if (target.startsWith("/report.css")) {
            response.setContentType("text/css;charset=utf-8");
            baseRequest.setHandled(true);

            InputStream a = this.getClass().getResourceAsStream("/servlet/resources/report.css");
            byte[] buff = new byte[1024];
            int read;
            while ((read = a.read(buff)) > 0) {
                response.getOutputStream().write(buff,0,read);
            }

        } else {
            response.setContentType("text/html;charset=utf-8");
            response.setStatus(HttpServletResponse.SC_OK);
            baseRequest.setHandled(true);
            response.getWriter().println(nodeInfoCollector.getReport());
        }
    }

    public static void startServer(NodeInfoCollector nodeInfoCollector) throws Exception {
        Server server = new Server(80);
        server.setHandler(new Servlet(nodeInfoCollector));

        server.start();
        server.join();
    }
}