package org.chuma.homecontroller.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.chuma.homecontroller.controller.nodeinfo.NodeInfoCollector;
import org.chuma.homecontroller.app.configurator.AbstractConfigurator;
import org.chuma.homecontroller.app.configurator.MartinConfigurator;
import org.chuma.homecontroller.app.configurator.PiConfigurator;
import org.chuma.homecontroller.app.configurator.PiPeConfigurator;
import org.chuma.homecontroller.app.servlet.Servlet;
import org.chuma.homecontroller.base.packet.IPacketUartIO;
import org.chuma.homecontroller.base.packet.PacketUartIO;
import org.chuma.homecontroller.base.packet.PacketUartIOException;
import org.chuma.homecontroller.base.packet.PacketUartIOMock;

import java.net.InetAddress;

public class Main {
    static Logger log = LoggerFactory.getLogger(Main.class.getName());

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
            if (hostName.equalsIgnoreCase("raspberrypi") || hostName.equalsIgnoreCase("pchuchma")) {
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
