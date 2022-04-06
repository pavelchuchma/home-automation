package org.chuma.homecontroller.app.servlet.simulation;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.slf4j.event.Level;

import org.chuma.homecontroller.app.servlet.rest.impl.JsonWriter;
import org.chuma.homecontroller.app.servlet.ws.AbstractWebSocketAdapter;
import org.chuma.homecontroller.app.servlet.ws.AbstractWebSocketHandler;
import org.chuma.homecontroller.base.node.Pin;
import org.chuma.homecontroller.base.packet.simulation.SimulatedNode;
import org.chuma.homecontroller.base.packet.simulation.SimulatedNodeListener;
import org.chuma.homecontroller.base.packet.simulation.SimulatedPacketUartIO;

import static org.chuma.homecontroller.base.packet.simulation.SimulatedPacketUartIO.PORT_ADDRESS;
import static org.chuma.homecontroller.base.packet.simulation.SimulatedPacketUartIO.TRIS_ADDRESS;

public class SimulationWebSocketHandler extends AbstractWebSocketHandler implements SimulatedNodeListener {
    private SimulatedPacketUartIO simulator;
    // Connected clients
    private Set<Adapter> clients = ConcurrentHashMap.newKeySet();

    public SimulationWebSocketHandler(SimulatedPacketUartIO simulator) {
        super("/simulation");
        this.simulator = simulator;
        simulator.addListener(this);
    }

    private void processEvent(Consumer<Adapter> action) {
        for (Iterator<Adapter> it = clients.iterator(); it.hasNext();) {
            Adapter a = it.next();
            if (a.isConnected()) {
                action.accept(a);
            } else {
                it.remove();
            }
        }
    }

    @Override
    public void logMessage(SimulatedNode node, Level level, String messageFormat, Object... args) {
        processEvent(a -> {
            a.logMessage(node, level, messageFormat, args); 
         });
    }

    @Override
    public void onSetPort(SimulatedNode node, int port, int value) {
        processEvent(a -> {
           a.onSetPort(node, port, value); 
        });
    }

    @Override
    public void onSetTris(SimulatedNode node, int port, int value) {
        processEvent(a -> {
            a.onSetTris(node, port, value); 
         });
    }

    @Override
    public void onSetEventMask(SimulatedNode node, int port, int mask) {
        processEvent(a -> {
            a.onSetEventMask(node, port, mask); 
         });
    }

    @Override
    public void onSetManualPwm(SimulatedNode node, int port, int pin, int value) {
        processEvent(a -> {
            a.onSetManualPwm(node, port, pin, value); 
         });
    }

    @Override
    public WebSocketAdapter newAdapter() {
        return new Adapter();
    }

    private class Adapter extends AbstractWebSocketAdapter {
        private ConcurrentMap<Integer, NodeData> nodes = new ConcurrentHashMap<>();

        @Override
        public void onWebSocketConnect(Session session) {
            super.onWebSocketConnect(session);
            // Connected - remember current state so we can detect when to notify
            for (SimulatedNode n : simulator.getSimulatedNodes()) {
                NodeData data = new NodeData(n);
                for (int i = 0; i < PORT_ADDRESS.length; i++) {
                    data.port[i] = n.readRam(PORT_ADDRESS[i]);
                }
                for (int i = 0; i < TRIS_ADDRESS.length; i++) {
                    data.tris[i] = n.readRam(TRIS_ADDRESS[i]);
                }
                nodes.put(n.getId(), data);
            }
            // Register itself to parent for events
            clients.add(this);
        }

        @Override
        public void onWebSocketClose(int statusCode, String reason) {
            // Unregister itself from parent
            clients.remove(this);
            super.onWebSocketClose(statusCode, reason);
        }

        @Override
        public void onWebSocketText(String message) {
            super.onWebSocketText(message);
            // Receives message in format "node.pin"
            int i = message.indexOf('.');
            if (i > 0) {
                try {
                    int id = Integer.parseInt(message.substring(0, i));
                    Pin pin = Pin.fromString(message.substring(i + 1));
                    NodeData n = nodes.get(id);
                    if (n != null) {
                        // Toggle pin
                        n.node.setInputPin(pin, -1);
                    }
                } catch (NumberFormatException e) {
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
            }
        }

        void logMessage(SimulatedNode node, Level level, String messageFormat, Object... args) {
            // TODO
        }

        void onSetPort(SimulatedNode node, int port, int value) {
            NodeData data = nodes.get(node.getId());
            if (data != null) {
                generatePortChanges(node, "value", data.port, port, value);
            }
        }

        void onSetTris(SimulatedNode node, int port, int value) {
            NodeData data = nodes.get(node.getId());
            if (data != null) {
                generatePortChanges(node, "dir", data.tris, port, value);
            }
        }

        void onSetEventMask(SimulatedNode node, int port, int mask) {
            // TODO Auto-generated method stub
        }

        void onSetManualPwm(SimulatedNode node, int port, int pin, int value) {
            JsonWriter w = new JsonWriter(false);
            w.startObject();
            w.startArrayAttribute("changes");
            w.startObject();
            w.addAttribute("id", node.getId() + "." + (char)(port + 'A') + Integer.toString(pin));
            w.addAttribute("what", "value");
            w.addAttribute("value", (int)(value * 100 / 48) + "%");
            w.close();
            w.close();
            w.close();
            sendText(w.toString());
        }

        void generatePortChanges(SimulatedNode node, String what, int[] dataArray, int port, int value) {
            int old = dataArray[port];
            dataArray[port] = value;
            int changed = old ^ value;
            if (changed != 0) {
                // Something changed - notify
                JsonWriter w = new JsonWriter(false);
                w.startObject();
                w.startArrayAttribute("changes");
                for (int i = 0; i < 8; i++, changed >>= 1, value >>= 1) {
                    if ((changed & 1) == 1) {
                        w.startObject();
                        w.addAttribute("id", node.getId() + "." + (char)(port + 'A') + Integer.toString(i));
                        w.addAttribute("what", what);
                        w.addAttribute("value", Integer.toString(value & 1));
                        w.close();
                    }
                }
                w.close();
                w.close();
                sendText(w.toString());
            }
        }
    }

    private static class NodeData {
        SimulatedNode node;
        int[] port = new int[PORT_ADDRESS.length];
        int[] tris = new int[TRIS_ADDRESS.length];

        public NodeData(SimulatedNode node) {
            this.node = node;
        }
    }
}
