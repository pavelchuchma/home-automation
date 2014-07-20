package servlet;

import app.NodeInfoCollector;
import controller.Action.Action;
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
                response.getOutputStream().write(buff, 0, read);
            }
        } else if (target.startsWith("/restart")) {
            System.exit(100);
        } else if (target.startsWith("/zaluzie")) {
            if (target.startsWith("/zaluzie/a")) {
                int actionIndex = Integer.parseInt(target.substring("/zaluzie/a".length()));
                zaluezieActions[actionIndex].perform();
            }

            response.setContentType("text/html;charset=utf-8");
            response.setStatus(HttpServletResponse.SC_OK);
            baseRequest.setHandled(true);
            response.getWriter().println(getZaluziePage());

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
    public static Action[] zaluezieActions;

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

    private String getZaluziePage() {
        StringBuilder builder = new StringBuilder();

        builder.append("<html>" +
                "<head>" +
                "<link href='/report.css' rel='stylesheet' type='text/css'/>\n" +
                "</head>" +
                "<body><a href='/zaluzie'>Refresh</a>&nbsp;&nbsp;&nbsp;&nbsp;<a href='/'>Back</a>\n");

        builder.append(getZaluzieTable(0, 6));

        builder.append("</body></html>");
        return builder.toString();
    }

    private String getZaluzieTable(int startIndex, int count) {
        StringBuilder builder = new StringBuilder();
        builder.append("<br/><br/><table class='buttonTable'><tr>");
        for (int i = startIndex; i < startIndex + count; i += 2) {
            String fieldClass = (zaluezieActions[i].getActor().getValue() == 0) ? "zaluzieUp" : "zaluzieUpRunning";
            builder.append(String.format("<td class='%s'><a href='/zaluzie/a%d'>%s</a>", fieldClass, i, zaluezieActions[i].getActor().getId()));
        }
        builder.append("<tr>");
        for (int i = startIndex + 1; i < startIndex + count; i += 2) {
            String fieldClass = (zaluezieActions[i].getActor().getValue() == 0) ? "zaluzieDown" : "zaluzieDownRunning";
            builder.append(String.format("<td class='%s'><a href='/zaluzie/a%d'>%s</a>", fieldClass, i, zaluezieActions[i].getActor().getId()));
        }
        builder.append("</table>");
        return builder.toString();
    }
}