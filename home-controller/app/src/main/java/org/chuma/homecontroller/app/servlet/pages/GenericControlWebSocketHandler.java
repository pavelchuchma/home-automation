package org.chuma.homecontroller.app.servlet.pages;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;

import org.chuma.homecontroller.app.servlet.rest.impl.JsonWriter;
import org.chuma.homecontroller.app.servlet.ws.AbstractWebSocketAdapter;
import org.chuma.homecontroller.app.servlet.ws.AbstractWebSocketHandler;
import org.chuma.homecontroller.base.node.Node;
import org.chuma.homecontroller.base.node.Pin;
import org.chuma.homecontroller.controller.nodeinfo.NodeInfo;
import org.chuma.homecontroller.controller.nodeinfo.NodeInfoRegistry;

public class GenericControlWebSocketHandler extends AbstractWebSocketHandler implements Node.Listener {
    private NodeInfoRegistry nodeInfoRegistry;
    // Connected clients
    private Set<Adapter> clients = ConcurrentHashMap.newKeySet();

    public GenericControlWebSocketHandler(NodeInfoRegistry nodeInfoRegistry) {
        super("/generic-control");
        this.nodeInfoRegistry = nodeInfoRegistry;
        for (NodeInfo nodeInfo : nodeInfoRegistry.getNodeInfos()) {
            nodeInfo.getNode().addListener(this);
        }
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
    public void onButtonDown(Node node, Pin pin, int upTime) {
        processEvent(a -> {
            a.sendPinChange(node, pin, 0);
        });
    }

    @Override
    public void onButtonUp(Node node, Pin pin, int downTime) {
        processEvent(a -> {
            a.sendPinChange(node, pin, 1);
        });
    }

    @Override
    public void onReboot(Node node, int pingCounter, int rconValue) throws IOException {
    }

    @Override
    public void onInitialized(Node node) {
        // TODO: Read current node values
    }

    @Override
    public WebSocketAdapter newAdapter() {
        return new Adapter();
    }

    private class Adapter extends AbstractWebSocketAdapter {
        @Override
        public void onWebSocketConnect(Session session) {
            super.onWebSocketConnect(session);
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
            int j = message.indexOf('=');
            if (i > 0 && j > i) {
                try {
                    int id = Integer.parseInt(message.substring(0, i));
                    Pin pin = Pin.fromString(message.substring(i + 1, j));
                    String value = message.substring(j + 1);
                    // TODO: HOWTO detect which pin is PWM?
                    Node n = nodeInfoRegistry.getNode(id);
                    if (n != null) {
                        if (value.endsWith("%")) {
                            // PWM
                            int v = Integer.parseInt(value.substring(0, value.length() - 1));
                            v = v * 48 / 100;
                            n.setManualPwmValue(pin, v);
                        } else {
                            // Value
                            n.setPinValue(pin, Integer.parseInt(value));
                        }
                    }
                } catch (NumberFormatException e) {
                } catch (IllegalArgumentException | IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void sendPinChange(Node node, Pin pin, int value) {
            // Something changed - notify
            JsonWriter w = new JsonWriter(false);
            w.startObject();
            w.startArrayAttribute("changes");
            w.startObject();
            w.addAttribute("id", node.getNodeId() + "." + pin.getPort() + Integer.toString(pin.getPinIndex()));
            w.addAttribute("value", Integer.toString(value & 1));
            w.close();
            w.close();
            w.close();
            sendText(w.toString());
        }
    }
}
