package controller.controller;

import controller.actor.IOnOffActor;
import junit.framework.Assert;
import org.junit.Test;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class LouversControllerImplTest {
    class ActionItem {
        IOnOffActor actor;
        String actionName;
        int value;
        int tolerance;

        ActionItem(IOnOffActor actor, String actionName, int value) {
            this(actor, actionName, value, 0);
        }

        ActionItem(IOnOffActor actor, String actionName, int value, int tolerance) {
            this.actor = actor;
            this.actionName = actionName;
            this.value = value;
            this.tolerance = tolerance;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ActionItem that = (ActionItem) o;

            if (Math.abs(value - that.value) > tolerance) return false;
            if (actionName != null ? !actionName.equals(that.actionName) : that.actionName != null) return false;
            if (actor != that.actor) return false;

            return true;
        }

        @Override
        public String toString() {
            return "ActionItem{" +
                    "actor=" + actor +
                    ", actionName='" + actionName + '\'' +
                    ", value=" + value +
                    "(" + tolerance + ")" +
                    '}';
        }
    }

    List<ActionItem> actions = new ArrayList<>();
    IOnOffActor upActor = new Actor("UP");
    IOnOffActor downActor = new Actor("DOWN");

    class Actor implements IOnOffActor {
        boolean active = false;
        Object actionData;
        private String id;
        long switchOnTime;

        Actor(String id) {
            this.id = id;
        }

        @Override
        public boolean switchOn(int percent, Object actionData) {
            active = true;
            this.actionData = actionData;
            switchOnTime = System.currentTimeMillis();
            actions.add(new ActionItem(this, "on", 0));
            return true;
        }

        @Override
        public boolean switchOff(Object actionData) {
            active = false;
            this.actionData = actionData;

            long duration = (switchOnTime == 0) ? 0 : System.currentTimeMillis() - switchOnTime;
            actions.add(new ActionItem(this, "off", (int) duration));

            switchOnTime = 0;
            return true;
        }

        @Override
        public boolean isOn() {
            return active;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public boolean setValue(int val, Object actionData) {
            throw new NotImplementedException();
        }

        @Override
        public Object getLastActionData() {
            return actionData;
        }

        @Override
        public void removeActionData() {
            actionData = null;
        }

        @Override
        public int getValue() {
            throw new NotImplementedException();
        }

        @Override
        public void setActionData(Object actionData) {
            this.actionData = actionData;
        }

        @Override
        public void setIndicatorsAndActionData(boolean invert, Object actionData) {
        }

        @Override
        public String toString() {
            return "Actor{" +
                    "id='" + id + '\'' +
                    '}';
        }
    }

    @Test
    public void testUp() throws Exception {
        LouversControllerImpl lc = new LouversControllerImpl("LC", upActor, downActor, 10, 100, 0);

        lc.up();

        Assert.assertEquals(3, actions.size());
        Iterator<ActionItem> iterator = actions.iterator();
        Assert.assertEquals(new ActionItem(downActor, "off", 0), iterator.next());
        Assert.assertEquals(new ActionItem(upActor, "on", 0), iterator.next());
        Assert.assertEquals(new ActionItem(upActor, "off", 100, 2), iterator.next());
        Assert.assertTrue(!iterator.hasNext());
    }

    @Test
    public void testDown() throws Exception {
        LouversControllerImpl lc = new LouversControllerImpl("LC", upActor, downActor, 10, 100, 0);

        lc.blind();

        Iterator<ActionItem> iterator = actions.iterator();
        Assert.assertEquals(new ActionItem(upActor, "off", 0), iterator.next());
        Assert.assertEquals(new ActionItem(downActor, "on", 0), iterator.next());
        Assert.assertEquals(new ActionItem(downActor, "off", 100, 2), iterator.next());
        Assert.assertTrue(!iterator.hasNext());
    }

    @Test
    public void testDownAndOutshine() throws Exception {
        LouversControllerImpl lc = new LouversControllerImpl("LC", upActor, downActor, 10, 100, 0);

        lc.blind();

        actions.clear();

        lc.outshine(0);
        int pos = lc.louversPosition.getPosition();
        int offset = lc.louversPosition.getOffset();
        Assert.assertTrue("pos: " + pos, pos >= 87 && pos <= 91);
        Assert.assertEquals(0, offset);

        lc.outshine(50);
        pos = lc.louversPosition.getPosition();
        offset = lc.louversPosition.getOffset();
        Assert.assertTrue("pos: " + pos, pos >= 94 && pos <= 96);
        Assert.assertEquals(5, offset);

        lc.outshine(70);
        pos = lc.louversPosition.getPosition();
        offset = lc.louversPosition.getOffset();
        Assert.assertTrue("pos: " + pos, pos >= 96 && pos <= 98);
        Assert.assertEquals(7, offset);

        lc.outshine(30);
        pos = lc.louversPosition.getPosition();
        offset = lc.louversPosition.getOffset();
        Assert.assertTrue("pos: " + pos, pos >= 92 && pos <= 94);
        Assert.assertEquals(3, offset);

        Iterator<ActionItem> iterator = actions.iterator();
        Assert.assertEquals(new ActionItem(downActor, "off", 0), iterator.next());
        Assert.assertEquals(new ActionItem(upActor, "on", 0), iterator.next());
        Assert.assertEquals(new ActionItem(upActor, "off", 10), iterator.next());


//        Assert.assertTrue(!iterator.hasNext());
    }

    @Test
    public void testDownAndStop() throws Exception {

        LouversControllerImpl lc = new LouversControllerImpl("LC", upActor, downActor, 10, 100, 0);
        lc.up();

        new Thread(() -> {
            waitNoException(30);
            lc.stop();
        }).start();

        long start = System.currentTimeMillis();
        lc.blind();
        long duration = System.currentTimeMillis() - start;
        Assert.assertTrue("duration: " + duration, duration >= 30 && duration < 38);

        Iterator<ActionItem> iterator = actions.iterator();

        Assert.assertEquals(new ActionItem(downActor, "off", 0), iterator.next());
        Assert.assertEquals(new ActionItem(upActor, "on", 0), iterator.next());
        Assert.assertEquals(new ActionItem(upActor, "off", 100, 2), iterator.next());


        Assert.assertEquals(new ActionItem(upActor, "off", 0), iterator.next());
        Assert.assertEquals(new ActionItem(downActor, "on", 0), iterator.next());
        Assert.assertEquals(new ActionItem(downActor, "off", 30, 4), iterator.next());
        Assert.assertTrue(!iterator.hasNext());

        int pos = lc.louversPosition.getPosition();
        Assert.assertTrue("pos: " + pos, pos >= 30 && pos < 35);
        Assert.assertEquals(10, lc.louversPosition.getOffset());
    }

    @Test
    public void testDownAndUp() throws Exception {

        LouversControllerImpl lc = new LouversControllerImpl("LC", upActor, downActor, 10, 100, 5);
        lc.up();

        Thread thread = new Thread(() -> {
            waitNoException(30);
            lc.up();
        });
        thread.start();

        long start = System.currentTimeMillis();
        lc.blind();
        long duration = System.currentTimeMillis() - start;
        Assert.assertTrue("duration: " + duration, duration >= 30 && duration < 38);

        thread.join();
        Iterator<ActionItem> iterator = actions.iterator();

        // up()
        Assert.assertEquals(new ActionItem(downActor, "off", 0), iterator.next());
        Assert.assertEquals(new ActionItem(upActor, "on", 0), iterator.next());
        Assert.assertEquals(new ActionItem(upActor, "off", 105, 2), iterator.next()); // 100 ms + 5 ms upReserve

        // down()
        Assert.assertEquals(new ActionItem(upActor, "off", 0), iterator.next());
        Assert.assertEquals(new ActionItem(downActor, "on", 0), iterator.next());

        // up() from another thread after 30 ms
        Assert.assertEquals(new ActionItem(downActor, "off", 30, 4), iterator.next());
        Assert.assertEquals(new ActionItem(upActor, "on", 0), iterator.next());
        Assert.assertEquals(new ActionItem(upActor, "off", 35, 2), iterator.next()); //30 ms of position + 5 ms upReserve


        Assert.assertTrue(!iterator.hasNext());

        Assert.assertEquals(0, lc.louversPosition.getPosition());
        Assert.assertEquals(0, lc.louversPosition.getOffset());
    }

    private void waitNoException(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}