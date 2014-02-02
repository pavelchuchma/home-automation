package controller;

import app.NodeInfoCollector;
import node.Node;
import node.Pin;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class Controller {
    NodeInfoCollector collector;
    ConcurrentHashMap<String, Switch> switchMap = new ConcurrentHashMap<String, Switch>();

    public Controller() {

    }

    void addSwitch(Switch sw) {
        //switchMap.put(Switch.createKeyString(sw), sw);
    }

    Node.Listener l= new Node.Listener() {
        @Override
        public void onButtonDown(Node node, Pin pin) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public void onButtonUp(Node node, Pin pin, int downTime) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public void onReboot(Node node, int pingCounter, int rconValue) throws IOException, IllegalArgumentException {
            //To change body of implemented methods use File | Settings | File Templates.
        }
    };

}