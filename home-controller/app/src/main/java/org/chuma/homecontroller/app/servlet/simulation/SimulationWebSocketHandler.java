package org.chuma.homecontroller.app.servlet.simulation;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
    private final SimulatedPacketUartIO simulator;
    // Connected clients
    private final Set<Adapter> clients = ConcurrentHashMap.newKeySet();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public SimulationWebSocketHandler(SimulatedPacketUartIO simulator) {
        super("/simulation");
        this.simulator = simulator;
        simulator.addListener(this);
    }

    private void processEvent(Consumer<Adapter> action) {
        for (Iterator<Adapter> it = clients.iterator(); it.hasNext();) {
            Adapter a = it.next();
            if (a.isConnected()) {
                executor.execute(() -> action.accept(a));
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
            // Connected - send and remember current state so we can detect when to notify
            JsonWriter w = new JsonWriter(false);
            w.startObject();
            w.startArrayAttribute("changes");
            for (SimulatedNode n : simulator.getSimulatedNodes()) {
                NodeData data = new NodeData(n);
                for (int port = 0; port < PORT_ADDRESS.length; port++) {
                    data.port[port] = n.readRam(PORT_ADDRESS[port]);
                    // Check if any of pin is in PWM - send PWM state instead
                    int mask = 0;
                    for (int pin = 0; pin < 8; pin++) {
                        int pwm = n.getManualPwm(port, pin);
                        if (pwm >= 0) {
                            addPwmChange(w, n, port, pin, pwm);
                        } else {
                            mask |= 1 << pin;
                        }
                    }
                    // Send remaining pins (those in mask)
                    addChanges(w, n, "value", port, data.port[port], mask);
                }
                for (int i = 0; i < TRIS_ADDRESS.length; i++) {
                    data.tris[i] = n.readRam(TRIS_ADDRESS[i]);
                    addChanges(w, n, "dir", i, data.tris[i], 0xff);
                }
                nodes.put(n.getId(), data);
            }
            w.close();
            w.close();
            // Register itself to parent for events
            clients.add(this);
            // Send current state
            sendText(w.toString());
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
            JsonWriter w = new JsonWriter(false);
            w.startObject();
            w.startObjectAttribute("log");
            w.addAttribute("node", node.getId());
            w.addAttribute("message", String.format("%s: %5s: %s", new SimpleDateFormat("HH:mm:ss.SSS").format(new Date()), level, String.format(messageFormat, args)));
            w.close();
            w.close();
            sendText(w.toString());
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
            addPwmChange(w, node, port, pin, value);
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
                addChanges(w, node, what, port, value, changed);
                w.close();
                w.close();
                sendText(w.toString());
            }
        }

        private void addPwmChange(JsonWriter w, SimulatedNode node, int port, int pin, int value) {
            w.startObject();
            w.addAttribute("id", node.getId() + "." + (char)(port + 'A') + Integer.toString(pin));
            w.addAttribute("what", "value");
            w.addAttribute("value", (int)(value * 100 / 48) + "%");
            w.close();
        }

        private void addChanges(JsonWriter w, SimulatedNode node, String what, int port, int value, int changed) {
            for (int i = 0; i < 8; i++, changed >>= 1, value >>= 1) {
                if ((changed & 1) == 1) {
                    w.startObject();
                    w.addAttribute("id", node.getId() + "." + (char)(port + 'A') + Integer.toString(i));
                    w.addAttribute("what", what);
                    w.addAttribute("value", Integer.toString(value & 1));
                    w.close();
                }
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
