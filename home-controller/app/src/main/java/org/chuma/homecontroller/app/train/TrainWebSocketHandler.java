package org.chuma.homecontroller.app.train;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;

import org.chuma.homecontroller.app.servlet.rest.impl.JsonWriter;
import org.chuma.homecontroller.app.servlet.ws.AbstractWebSocketAdapter;
import org.chuma.homecontroller.app.servlet.ws.AbstractWebSocketHandler;

public class TrainWebSocketHandler extends AbstractWebSocketHandler {
    private TrainSwitch trainSwitch;
    // Connected clients
    private Set<Adapter> clients = ConcurrentHashMap.newKeySet();

    public TrainWebSocketHandler(TrainSwitch trainSwitch) {
        super("/train");
        this.trainSwitch = trainSwitch;
        trainSwitch.addListener(ts -> {
            processEvent(a -> a.sendSwitchState(ts));
        });
    }

    private void processEvent(Consumer<Adapter> action) {
        for (Iterator<Adapter> it = clients.iterator(); it.hasNext();) {
            Adapter a = it.next();
            if (a.isConnected()) {
                new Thread(() -> action.accept(a)).start();
            } else {
                it.remove();
            }
        }
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
            // Read initial state
            sendSwitchState(trainSwitch);
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
            // Receives message in format "function:value"
            int i = message.indexOf(':');
            if (i > 0) {
                try {
                    String action = message.substring(0, i);
                    int value = Integer.parseInt(message.substring(i + 1));
                    // Perform action
                    switch(action) {
                        case "switch":
                            if (trainSwitch.isStraight()) {
                                trainSwitch.switchTurn();
                            } else {
                                trainSwitch.switchStraight();
                            }
                            break;
                        case "power":
                        case "pwm":
                        case "auto":
                            System.out.println("COMMAND: " + action + " -> " + value); // TODO
                    }
                } catch (NumberFormatException e) {
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        public void sendSwitchState(TrainSwitch trainSwitch) {
            int dir;
            try {
                dir = trainSwitch.isStraight() ? 0 : 1;
            } catch (Exception e) {
                dir = -1;
            }
            JsonWriter w = new JsonWriter(false);
            w.startObject();
            w.addAttribute("action", "switch");
            w.addAttribute("dir", dir);
            w.close();
            sendText(w.toString());
        }
    }
}
