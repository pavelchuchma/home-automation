package org.chuma.homecontroller.base.packet.simulation;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.event.Level;

import org.chuma.homecontroller.base.node.Node;
import org.chuma.homecontroller.base.node.Pic;
import org.chuma.homecontroller.base.node.Pin;

import static org.chuma.homecontroller.base.packet.simulation.SimulatedPacketUartIO.PORT_ADDRESS;
import static org.chuma.homecontroller.base.packet.simulation.SimulatedPacketUartIO.TRIS_ADDRESS;

/**
 * Implementation of simulated node. Handles memory read/write, PWM setting, event mask and heart beat.
 * By default initializes all ports' TRIS to 0xff (all input) and PORT to 0. You can call {@link #initializePort(int, int)}
 * to set different initial value for ports.
 */
public class SimulatedNode {
    private final SimulatedPacketUartIO simulator;
    private final int id;
    private final Node node;
    private final SimulatedNodeListener listener;
    private final ConcurrentMap<Integer, Integer> memory = new ConcurrentHashMap<>();
    private final int[][] pwm = new int[TRIS_ADDRESS.length][8];
    private final int[] eventMask = new int[TRIS_ADDRESS.length];
    private int heartBeatPeriod = 20;
    private long nextHeartBeat = 0;

    public SimulatedNode(SimulatedPacketUartIO simulator, int id, Node node, SimulatedNodeListener listener) {
        this.simulator = simulator;
        this.id = id;
        this.node = node;
        this.listener = listener;
        // Initialize memory - TRIS to all input
        memory.put(Pic.TRISA, 0xff);
        memory.put(Pic.TRISB, 0xff);
        memory.put(Pic.TRISC, 0xff);
        memory.put(Pic.TRISD, 0xff);
        memory.put(Pic.TRISE, 0xff);
        memory.put(Pic.PORTA, 0);
        memory.put(Pic.PORTB, 0);
        memory.put(Pic.PORTC, 0);
        memory.put(Pic.PORTD, 0);
        memory.put(Pic.PORTE, 0);
        // Initialize PWM to -1 - not enabled on pin
        for (int i = 0; i < pwm.length; i++) {
            for (int j = 0; j < pwm[i].length; j++) {
                pwm[i][j] = -1;
            }
        }
    }

    /**
     * Get node ID.
     */
    public int getId() {
        return id;
    }

    /**
     * Get {@link Node} instance if available.
     */
    public Node getNode() {
        return node;
    }

    /**
     * Initialize port values without any checks. Should be called before real simulation starts.
     */
    public void initializePort(int port, int value) {
        memory.put(PORT_ADDRESS[port], value & 0xff);
    }

    /**
     * Check if address represents known port.
     * 
     * @return port (0 == A) or -1 if none
     */
    private int isPortAddress(int address, int[] knownAddresses) {
        for (int i = 0; i < knownAddresses.length; i++) {
            if (knownAddresses[i] == address) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Set input pin value.
     *
     * @param pin pin to set
     * @param value value to set (0, 1) or toggle (-1)
     */
    public void setInputPin(Pin pin, int value) {
        int port = pin.getPortIndex();
        int mask = pin.getBitMask();
        if ((memory.get(TRIS_ADDRESS[port]) & mask) == 0) {
            // Output pin - do nothing
            return;
        }
        // Input pin - can set
        int old = memory.get(PORT_ADDRESS[port]);
        int v = old;
        switch (value) {
            case 0: v &= 0xff & ~mask;
            case 1: v |= 0xff & mask;
            case -1: v ^= 0xff & mask; 
        }
        if (v != old) {
            // Something to set - cannot use writeRam() as it checks TRIS
            memory.put(PORT_ADDRESS[port], v);
            listener.onSetPort(this, port, v);
            if ((eventMask[port] & mask) != 0) {
                // Send notification message
                simulator.sendPortPinChange(this, pin, mask, v & mask);
            }
        }
    }

    /**
     * Read RAM memory byte.
     */
    public int readRam(int address) {
        return memory.getOrDefault(address, 0);
    }

    /**
     * Write RAM memory byte. Returns value actually written.
     * The written value may be different from passed value because when writing
     * PORTx, bits set for input in corresponding TRISx won't be changed.
     */
    public int writeRam(int address, int value) {
        // If writing to PORTx, honor TRISx - for input, do not write, keep original value
        int port = isPortAddress(address, PORT_ADDRESS);
        if (port >= 0) {
            // Is port address for port i
            int tris = readRam(TRIS_ADDRESS[port]);
            // 1 in TRIS is input, so we must not change those with 1
            int v = readRam(address);
            value = ((v & tris) | (value & ~tris)) & 0xff;
        }
        memory.put(address, value);
        // Notify
        if (port >= 0) {
            listener.onSetPort(this, port, value);
        } else {
            int tris = isPortAddress(address, TRIS_ADDRESS);
            if (tris >= 0) {
                listener.onSetTris(this, tris, value);
            }
        }
        return value;
    }

    public void setEventMask(int port, int mask) {
        int tris = readRam(TRIS_ADDRESS[port]);
        if ((tris & mask) != mask) {
            listener.logMessage(this, Level.WARN, "wrong event mask %s for port %s - TRIS is %s", Node.asBinary(mask), (char)(port + 'A'), Node.asBinary(tris));
        }
        eventMask[port] = mask;
        listener.onSetEventMask(this, port, mask);
    }

    public int setManualPwm(int port, int pin, int v) {
        if (port < 0 || port > 2) {
            listener.logMessage(this, Level.WARN, "wrong port number - %d", port);
            return 1;
        }
        if (pin < 0 || pin > 7) {
            listener.logMessage(this, Level.WARN, "wrong pin number - %d", pin);
            return 1;
        }
        if (v < 0 || v > 48) {
            listener.logMessage(this, Level.WARN, "wrong pwm value - %d", v);
            return 1;
        }
        int tris = readRam(TRIS_ADDRESS[port]);
        if ((tris & (1 << pin)) != 0) {
            // Input mode - set to output
            writeRam(TRIS_ADDRESS[port], tris | (1 << pin));
        }
        pwm[port][pin] = v;
        listener.onSetManualPwm(this, port, pin, v);
        return 0;
    }

    /**
     * Get currently set PWM value for pin. Returns -1 if not in manual PWM mode.
     */
    public int getManualPwm(int port, int pin) {
        return pwm[port][pin];
    }

    public void setHeartBeatPeriod(int period) {
        heartBeatPeriod = period;
        resetHeartBeat();
    }

    /**
     * Reset next heart beat time. Should be called after sending heart beat.
     */
    public void resetHeartBeat() {
        nextHeartBeat = System.currentTimeMillis() + heartBeatPeriod * 1000;
    }

    /**
     * Check whether heartbeat should be sent. After sending, {@link #resetHeartBeat()} should be called.
     */
    public boolean shouldSendHeartBeat() {
        return nextHeartBeat <= System.currentTimeMillis();
    }
}
