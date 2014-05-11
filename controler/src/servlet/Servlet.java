package servlet;

import app.NodeInfoCollector;
import controller.Action.Action;
import node.Node;
import node.Pic;
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
        } else if (target.startsWith("/restart")) {
            System.exit(100);
        } else {
            if (target.startsWith("/a")) {
                processAction(target);
            }
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

    public static Action action1;
    public static Action action2;
    public static Action action3;
    public static Action action4;
    public static Action action5;

    private void processAction(String action) {
        if (action.startsWith("/a1") && action1 != null) {
            action1.perform();
        }
        if (action.startsWith("/a2") && action2 != null) {
            action2.perform();
        }
        if (action.startsWith("/a3") && action3 != null) {
            action3.perform();
        }
        if (action.startsWith("/a4") && action4 != null) {
            action4.perform();
        }
        if (action.startsWith("/a5") && action5 != null) {
            action5.perform();
        }
    }
}