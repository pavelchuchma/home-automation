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
import org.chuma.homecontroller.app.train.TrainControl.Listener;

public class TrainWebSocketHandler extends AbstractWebSocketHandler {
    private static final int SPEED_MIN = 60;
    private static final int SPEED_STEP = 10;

    private TrainSwitch trainSwitch;
    private TrainControl trainControl;
    // Connected clients
    private Set<Adapter> clients = ConcurrentHashMap.newKeySet();

    public TrainWebSocketHandler(TrainSwitch trainSwitch, TrainControl trainControl) {
        super("/train");
        this.trainSwitch = trainSwitch;
        this.trainControl = trainControl;
        trainSwitch.addListener(ts -> {
            processEvent(a -> a.sendSwitchState(ts));
        });
        trainControl.addListener(new Listener() {
            @Override
            public void speedChanged(int speed) {
                processEvent(a -> a.sendSpeedChange(speed));
            }
            
            @Override
            public void directionChanged(int dir) {
                processEvent(a -> a.sendDirectionChange(dir));
            }
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
            sendDirectionChange(trainControl.getDirection());
            sendSpeedChange(trainControl.getSpeed());
        }

        @Override
        public void onWebSocketClose(int statusCode, String reason) {
            // Unregister itself from parent
            clients.remove(this);
            super.onWebSocketClose(statusCode, reason);
        }

        @Override
        public void onWebSocketText(String message) {
            // TODO: HANDLE AUTO HERE - in case of auto, allow only stopping!
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
                        case "dir":
                            trainControl.setDirection(value);
                            break;
                        case "pwm":
                            int speed = trainControl.getSpeed();
                            System.out.println("OLD: " + speed + " - " + value);
                            if (speed == 0 && value > 0) {
                                speed = SPEED_MIN;
                            } else {
                                speed += value * SPEED_STEP;
                            }
                            if (speed > 100) {
                                speed = 100;
                            }
                            if (speed < SPEED_MIN) {
                                speed = 0;
                            }
                            System.out.println("NEW: " + speed);
                            trainControl.setSpeed(speed);
                            break;
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

        public void sendSpeedChange(int speed) {
            JsonWriter w = new JsonWriter(false);
            w.startObject();
            w.addAttribute("action", "pwm");
            w.addAttribute("value", speed);
            w.close();
            sendText(w.toString());
        }
        
        public void sendDirectionChange(int dir) {
            JsonWriter w = new JsonWriter(false);
            w.startObject();
            w.addAttribute("action", "dir");
            w.addAttribute("dir", dir);
            w.close();
            sendText(w.toString());
        }
    }
}
