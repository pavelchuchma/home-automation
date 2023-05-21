package org.chuma.homecontroller.app;

import java.net.InetAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.app.configurator.AbstractConfigurator;
import org.chuma.homecontroller.app.configurator.MartinConfigurator;
import org.chuma.homecontroller.app.configurator.Options;
import org.chuma.homecontroller.app.configurator.OptionsSingleton;
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
            Options options = OptionsSingleton.createInstance("cfg/app.properties", "default-app.properties");
            String bridgePortName = options.get("system.bridge.port");
            log.info("Using bridge serial port: {}", bridgePortName);
            IPacketUartIO packetUartIO;

            if ("simulator".equals(bridgePortName)) {
                packetUartIO = new SimulatedPacketUartIO();
            } else {
                packetUartIO = new PacketUartIO(bridgePortName, 19200);
            }

            NodeInfoRegistry nodeInfoRegistry = new NodeInfoRegistry(packetUartIO);

            String hostName = InetAddress.getLocalHost().getHostName();
            AbstractConfigurator configurator = getConfigurator(nodeInfoRegistry);
            log.info("Hostname: {}, using configurator: {}", hostName, configurator.getClass());
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

    private static AbstractConfigurator getConfigurator(NodeInfoRegistry nodeInfoRegistry) {
        String configurationName = OptionsSingleton.getInstance().get("system.application.configuration.name");
        switch (configurationName) {
            case "chuma":
                return new PiConfigurator(nodeInfoRegistry);
            case "martin":
                return new MartinConfigurator(nodeInfoRegistry);
            case "petr":
                return new PiPeConfigurator(nodeInfoRegistry);
        }
        throw new IllegalArgumentException("Unexpected application configuration name: '" + configurationName + "'");
    }
}
