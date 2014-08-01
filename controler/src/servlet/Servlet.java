package servlet;

import app.NodeInfo;
import app.NodeInfoCollector;
import controller.Action.Action;
import controller.device.ConnectedDevice;
import node.Node;
import node.Pic;
import node.Pin;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.GregorianCalendar;


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

        } else if (target.startsWith("/system")) {
            int debugNodeId = -1;
            if (target.startsWith("/system/r")) {
                int actionIndex = Integer.parseInt(target.substring("/system/r".length()));
                Node n = NodeInfoCollector.getInstance().getNode(actionIndex);
                n.reset();
            } else if (target.startsWith("/system/i")) {
                debugNodeId = Integer.parseInt(target.substring("/system/i".length()));
            }


            response.setContentType("text/html;charset=utf-8");
            response.setStatus(HttpServletResponse.SC_OK);
            baseRequest.setHandled(true);
            response.getWriter().println(getSystemPage(debugNodeId));

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
        builder.append(getZaluzieTable(6, 6));
        builder.append(getZaluzieTable(12, 6));

        builder.append("</body></html>");
        return builder.toString();
    }

    private String getSystemPage(int debugNodeId) {
        StringBuilder builder = new StringBuilder();
        Date resetSupportAdded = new GregorianCalendar(2014, 7, 1).getTime();

        builder.append("<html>" +
                "<head>" +
                "<link href='/report.css' rel='stylesheet' type='text/css'/>\n" +
                "</head>" +
                "<body><a href='/system'>Refresh</a>&nbsp;&nbsp;&nbsp;&nbsp;<a href='/'>Back</a>\n");

        builder.append("<br/><br/><table class='systemTable'><tr>");

        for (NodeInfo nodeInfo : nodeInfoCollector) {
            int nodeId = nodeInfo.getNode().getNodeId();

            Date buildTime = nodeInfo.getBuildTime();
            String resetLink = (buildTime == null || buildTime.after(resetSupportAdded)) ? String.format("<a href='/system/r%d'>reset</a>", nodeId) : "";
            builder.append(String.format("<tr><td>%d-%s<td><a href='/system/i%d'>info</a><td>%s", nodeId, nodeInfo.getNode().getName(), nodeId, resetLink));
        }
        builder.append("</table>");

        if (debugNodeId >= 0) {
            builder.append(printNodeDebugInfo(debugNodeId));
        }

        builder.append("</body></html>");
        return builder.toString();
    }

    private int applyBitMaskTo01(int values, int mask) {
        return ((values & mask) != 0) ? 1 : 0;
    }

    private String printNodeDebugInfo(int debugNodeId) {
        StringBuilder builder = new StringBuilder();

        Node node = nodeInfoCollector.getNode(debugNodeId);
        builder.append(String.format("<br/><br/><div class='nodeInfoTitle'>%d-%s Detail</div>", node.getNodeId(), node.getName()));

        int[] portValues = new int[3];
        int[] trisValues = new int[3];

        try {
            portValues[0] = node.readMemory(Pic.PORTA);
            trisValues[0] = node.readMemory(Pic.TRISA);
            portValues[1] = node.readMemory(Pic.PORTB);
            trisValues[1] = node.readMemory(Pic.TRISB);
            portValues[2] = node.readMemory(Pic.PORTC);
            trisValues[2] = node.readMemory(Pic.TRISC);
        } catch (IOException e) {
            builder.append(e);
            return builder.toString();
        }

//        portValues[0] = 0xFF;
//        trisValues[0] = 0xFF;
//        portValues[1] = 0x33;
//        trisValues[1] = 0x33;
//        portValues[2] = 0x11;
//        trisValues[2] = 0x11;


        builder.append("<table><tr>");
        for (int connId = 1; connId <= 3; connId++) {
            builder.append("<td class='nodeInfoConnectors'>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
            appendConnectorInfo(builder, portValues, trisValues, connId);
        }
        builder.append("</table>");

        appenPicPortInfo(builder, portValues, trisValues);


        return builder.toString();
    }

    private void appenPicPortInfo(StringBuilder builder, int[] portValues, int[] trisValues) {
        builder.append("<br/><table class='nodeInfoTable'><tr>");
        builder.append("<tr><th>Name<th>Tris<th>Value");
        char[] portNames = new char[]{'A', 'B', 'C'};
        for (int port = 0; port < 3; port++)
            for (int bit = 0; bit < 8; bit++) {
                builder.append(String.format("<tr><td>%c%d<td>%d<td>%d", portNames[port], bit, applyBitMaskTo01(trisValues[port], 1 << bit), applyBitMaskTo01(portValues[port], 1 << bit)));
            }
        builder.append("</table>");
    }

    private void appendConnectorInfo(StringBuilder builder, int[] portValues, int[] trisValues, int connId) {
        builder.append(String.format("<div class='nodeInfoConnectorTitle'>Conn #%d (T-V)</div>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;", connId));
        builder.append("<br/><table class='nodeInfoConnectorTable'><tr>");

        for (int row = 1; row <= 2; row++) {
            builder.append("<tr><td class='nodeInfoConnectorTable'>&nbsp;&nbsp;&nbsp;&nbsp;");
            for (int i = row; i < 7; i += 2) {
                appendConnPinDetail(builder, trisValues, portValues, connId, i);
            }
        }

        builder.append("</table>");
    }

    private void appendConnPinDetail(StringBuilder builder, int trisValues[], int portValues[], int connId, int pinId) {
        Pin pin = ConnectedDevice.getPin(connId, pinId);
        builder.append(String.format("<td class='nodeInfoConnectorTable'>%s %d-%d",
                pin.toString().substring(3),
                applyBitMaskTo01(trisValues[pin.getPortIndex()], pin.getBitMask()),
                applyBitMaskTo01(portValues[pin.getPortIndex()], pin.getBitMask())));
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