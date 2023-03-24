package org.chuma.homecontroller.app;

import java.net.InetAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.app.configurator.AbstractConfigurator;
import org.chuma.homecontroller.app.configurator.MartinConfigurator;
import org.chuma.homecontroller.app.configurator.PiConfigurator;
import org.chuma.homecontroller.app.configurator.PiPeConfigurator;
import org.chuma.homecontroller.app.servlet.Servlet;
import org.chuma.homecontroller.base.packet.IPacketUartIO;
import org.chuma.homecontroller.base.packet.PacketUartIO;
import org.chuma.homecontroller.base.packet.PacketUartIOException;
import org.chuma.homecontroller.base.packet.simulation.SimulatedPacketUartIO;
import org.chuma.homecontroller.controller.nodeinfo.NodeInfoRegistry;

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
                    packetUartIO = new SimulatedPacketUartIO();
                } else {
                    throw e;
                }
            }

            NodeInfoRegistry nodeInfoRegistry = new NodeInfoRegistry(packetUartIO);

            String hostName = InetAddress.getLocalHost().getHostName();
            AbstractConfigurator configurator;
            if (hostName.equalsIgnoreCase("raspberrypi") || hostName.equalsIgnoreCase("CZWW00145")) {
                configurator = new PiConfigurator(nodeInfoRegistry);
            } else if (hostName.equalsIgnoreCase("martinpi")) {
                configurator = new MartinConfigurator(nodeInfoRegistry);
            } else {
                configurator = new PiPeConfigurator(nodeInfoRegistry);
            }
            log.info("Hostname: " + hostName + " using configurator: " + configurator.getClass());
            configurator.configure();

            nodeInfoRegistry.start();

            packetUartIO.start();
            System.out.println("Listening ...");

            Servlet.startServer(configurator.getServlet());
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
