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
    Actor upActor = new Actor("UP");
    Actor downActor = new Actor("DOWN");

    class Actor implements IOnOffActor {
        boolean active = false;
        Object actionData;
        private String id;
        long switchOnTime;
        boolean broken = false;

        Actor(String id) {
            this.id = id;
        }

        public void breakIt() {
            broken = true;
        }

        @Override
        public boolean switchOn(int percent, Object actionData) {
            active = true;
            this.actionData = actionData;
            switchOnTime = System.currentTimeMillis();
            actions.add(new ActionItem(this, "on", 0));
            return !broken;
        }

        @Override
        public boolean switchOff(Object actionData) {
            active = false;
            this.actionData = actionData;

            long duration = (switchOnTime == 0) ? 0 : System.currentTimeMillis() - switchOnTime;
            actions.add(new ActionItem(this, "off", (int) duration));

            switchOnTime = 0;
            return !broken;
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
        public String getLabel() {
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
        public void callListenersAndSetActionData(boolean invert, Object actionData) {
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
        LouversControllerImpl lc = new LouversControllerImpl("lc", "LC", upActor, downActor, 100, 10);

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
        LouversControllerImpl lc = new LouversControllerImpl("lc", "LC", upActor, downActor, 100, 10);

        lc.blind();

        Iterator<ActionItem> iterator = actions.iterator();
        Assert.assertEquals(new ActionItem(upActor, "off", 0), iterator.next());
        Assert.assertEquals(new ActionItem(downActor, "on", 0), iterator.next());
        Assert.assertEquals(new ActionItem(downActor, "off", 100, 2), iterator.next());
        Assert.assertTrue(!iterator.hasNext());
    }

    @Test
    public void testDownAndOutshine() throws Exception {
        LouversControllerImpl lc = new LouversControllerImpl("lc", "LC", upActor, downActor, 1000, 100);

        lc.blind();

        actions.clear();

        lc.outshine(0);
        int pos = lc.louversPosition.getPosition();
        int offset = lc.louversPosition.getOffset();
        Assert.assertTrue("pos: " + pos, pos >= 891 && pos <= 905);
        Assert.assertEquals(0, offset);

        lc.outshine(50);
        pos = lc.louversPosition.getPosition();
        offset = lc.louversPosition.getOffset();
        Assert.assertTrue("pos: " + pos, pos >= 941 && pos <= 955);
        Assert.assertTrue("offset: " + offset, offset >= 41 && offset <= 59);

        lc.outshine(70);
        pos = lc.louversPosition.getPosition();
        offset = lc.louversPosition.getOffset();
        Assert.assertTrue("pos: " + pos, pos >= 961 && pos <= 975);
        Assert.assertTrue("offset: " + offset, offset >= 61 && offset <= 79);

        lc.outshine(30);
        pos = lc.louversPosition.getPosition();
        offset = lc.louversPosition.getOffset();
        Assert.assertTrue("pos: " + pos, pos >= 921 && pos <= 935);
        Assert.assertTrue("offset: " + offset, offset >= 21 && offset <= 39);

        Iterator<ActionItem> iterator = actions.iterator();
        Assert.assertEquals(new ActionItem(downActor, "off", 0), iterator.next());
        Assert.assertEquals(new ActionItem(upActor, "on", 0), iterator.next());
        Assert.assertEquals(new ActionItem(upActor, "off", 100, 5), iterator.next());


//        Assert.assertTrue(!iterator.hasNext());
    }

    @Test
    public void testDownAndOutshineAsync() throws Exception {
        LouversControllerImpl lc = new LouversControllerImpl("lc", "LC", upActor, downActor, 1000, 100);

        lc.up();

        actions.clear();


        Thread thread = new Thread(() -> {
            waitNoException(300);
            lc.outshine(40);
        });
        thread.start();
        long start = System.currentTimeMillis();
        lc.blind();
        thread.join();

        long duration = System.currentTimeMillis() - start;
        Assert.assertTrue("duration: " + duration, duration >= 1040 && duration <= 1100);

        int pos = lc.louversPosition.getPosition();
        Assert.assertTrue("pos: " + pos, pos >= 930 && pos <= 950);
        int offset = lc.louversPosition.getOffset();
        Assert.assertTrue("offset: " + offset, offset >= 31 && offset <= 48);


        Iterator<ActionItem> iterator = actions.iterator();
        // blind()
        Assert.assertEquals(new ActionItem(upActor, "off", 0), iterator.next());
        Assert.assertEquals(new ActionItem(downActor, "on", 0), iterator.next());
        // outshine(20%)
        Assert.assertEquals(new ActionItem(upActor, "off", 0), iterator.next());
        Assert.assertEquals(new ActionItem(downActor, "on", 0), iterator.next());
        Assert.assertEquals(new ActionItem(downActor, "off", 700, 15), iterator.next()); //remaining (1000-300)
//        Assert.assertEquals(new ActionItem(downActor, "off", 0), iterator.next());
        Assert.assertEquals(new ActionItem(upActor, "on", 0), iterator.next());
        Assert.assertEquals(new ActionItem(upActor, "off", 60, 10), iterator.next());


        Assert.assertTrue(!iterator.hasNext());
    }

    @Test
    public void testDownAndStop() throws Exception {

        LouversControllerImpl lc = new LouversControllerImpl("lc", "LC", upActor, downActor, 1000, 100);
        lc.up();

        new Thread(() -> {
            waitNoException(300);
            lc.stop();
        }).start();

        long start = System.currentTimeMillis();
        lc.blind();
        long duration = System.currentTimeMillis() - start;
        Assert.assertTrue("duration: " + duration, duration >= 300 && duration < 310);

        Iterator<ActionItem> iterator = actions.iterator();

        Assert.assertEquals(new ActionItem(downActor, "off", 0), iterator.next());
        Assert.assertEquals(new ActionItem(upActor, "on", 0), iterator.next());
        Assert.assertEquals(new ActionItem(upActor, "off", 1000, 5), iterator.next());


        Assert.assertEquals(new ActionItem(upActor, "off", 0), iterator.next());
        Assert.assertEquals(new ActionItem(downActor, "on", 0), iterator.next());
        Assert.assertEquals(new ActionItem(downActor, "off", 300, 10), iterator.next());
        Assert.assertTrue(!iterator.hasNext());

        int pos = lc.louversPosition.getPosition();
        Assert.assertTrue("pos: " + pos, pos >= 297 && pos < 310);
        Assert.assertEquals(100, lc.louversPosition.getOffset());
    }

    @Test
    public void testDownAndUp() throws Exception {

        LouversControllerImpl lc = new LouversControllerImpl("lc", "LC", upActor, downActor, 1000, 100);
        lc.up();

        Thread thread = new Thread(() -> {
            waitNoException(300);
            lc.up();
        });
        thread.start();

        long start = System.currentTimeMillis();
        lc.blind();
        long duration = System.currentTimeMillis() - start;
        Assert.assertTrue("duration: " + duration, duration >= 300 && duration < 310);

        thread.join();
        Iterator<ActionItem> iterator = actions.iterator();

        // up()
        Assert.assertEquals(new ActionItem(downActor, "off", 0), iterator.next());
        Assert.assertEquals(new ActionItem(upActor, "on", 0), iterator.next());
        Assert.assertEquals(new ActionItem(upActor, "off", 1005, 5), iterator.next()); // 100 ms + 5 ms upReserve

        // down()
        Assert.assertEquals(new ActionItem(upActor, "off", 0), iterator.next());
        Assert.assertEquals(new ActionItem(downActor, "on", 0), iterator.next());

        // up() from another thread after 30 ms
        Assert.assertEquals(new ActionItem(downActor, "off", 300, 5), iterator.next());
        Assert.assertEquals(new ActionItem(upActor, "on", 0), iterator.next());
        Assert.assertEquals(new ActionItem(upActor, "off", 305, 5), iterator.next()); //30 ms of position + 5 ms upReserve


        Assert.assertTrue(!iterator.hasNext());

        Assert.assertEquals(0, lc.louversPosition.getPosition());
        Assert.assertEquals(0, lc.louversPosition.getOffset());
    }

    @Test
    public void testDownAndUpBroken() throws Exception {

        LouversControllerImpl lc = new LouversControllerImpl("lc", "LC", upActor, downActor, 1000, 100);
        lc.up();

        Thread thread = new Thread(() -> {
            waitNoException(300);
            downActor.breakIt();
            lc.up();
        });
        thread.start();

        long start = System.currentTimeMillis();
        lc.blind();
        thread.join();
        long duration = System.currentTimeMillis() - start;
        Assert.assertTrue("duration: " + duration, duration >= 300 && duration < 330);

        Iterator<ActionItem> iterator = actions.iterator();

        // up()
        Assert.assertEquals(new ActionItem(downActor, "off", 0), iterator.next());
        Assert.assertEquals(new ActionItem(upActor, "on", 0), iterator.next());
        Assert.assertEquals(new ActionItem(upActor, "off", 1005, 5), iterator.next()); // 100 ms + 5 ms upReserve

        // down()
        Assert.assertEquals(new ActionItem(upActor, "off", 0), iterator.next());
        Assert.assertEquals(new ActionItem(downActor, "on", 0), iterator.next());

        // up() from another thread after 30 ms
        Assert.assertEquals(new ActionItem(downActor, "off", 300, 10), iterator.next());
        // broken downActor, no more actions
        Assert.assertTrue(!iterator.hasNext());

        Assert.assertEquals(-1, lc.louversPosition.getPosition());
        Assert.assertEquals(-1, lc.louversPosition.getOffset());
    }

    private void waitNoException(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}