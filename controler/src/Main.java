import java.net.InetAddress;

import app.NodeInfoCollector;
import app.configurator.AbstractConfigurator;
import app.configurator.MartinConfigurator;
import app.configurator.PiConfigurator;
import app.configurator.PiPeConfigurator;
import org.apache.log4j.Logger;
import packet.IPacketUartIO;
import packet.PacketUartIO;
import packet.PacketUartIOException;
import packet.PacketUartIOMock;
import servlet.Servlet;

public class Main {
    static Logger log = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        try {
            String port = (System.getenv("COMPUTERNAME") != null) ? "COM1" : "/dev/ttyS80";
            IPacketUartIO packetUartIO;
            try {
                packetUartIO = new PacketUartIO(port, 19200);
            } catch (Exception | Error e) {
                if (System.getenv("COMPUTERNAME") != null) {
                    packetUartIO = new PacketUartIOMock();
                } else {
                    throw e;
                }
            }

            NodeInfoCollector nodeInfoCollector = new NodeInfoCollector(packetUartIO);

            String hostName = InetAddress.getLocalHost().getHostName();
            AbstractConfigurator configurator;
            if (hostName.equalsIgnoreCase("raspberrypi") || hostName.equalsIgnoreCase("Agata")) {
                configurator = new PiConfigurator(nodeInfoCollector);
            } else if (hostName.equalsIgnoreCase("martinpi")) {
                configurator = new MartinConfigurator(nodeInfoCollector);
            } else {
                configurator = new PiPeConfigurator(nodeInfoCollector);
            }
            log.info("Hostname: " + hostName + " using configurator: " + configurator.getClass());
            configurator.configure();

            nodeInfoCollector.start();

            packetUartIO.start();
            System.out.println("Listening ...");

            Servlet.startServer(nodeInfoCollector);

        } catch (PacketUartIOException e) {
            log.error("Initialization failed", e);
            e.printStackTrace();
            System.exit(1);
        } catch (InterruptedException e) {
            log.error("Initialization failed", e);
            e.printStackTrace();
            System.exit(2);
        } catch (Exception | Error e) {
            log.error("Initialization failed", e);
            e.printStackTrace();
            System.exit(3);
        }
    }

}
