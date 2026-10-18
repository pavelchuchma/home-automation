# Nodes (PIC boards on the CAN bus)

How the PIC nodes work, how the controller talks to them through the bridge
node, and which pins are off limits. The node firmware lives next to this
project in `pic/node` (repo root); the boards are described in
[`pcb/pcb-boards.md`](../../pcb/pcb-boards.md).

## Topology

```
home-controller (Pi)  --USB/serial 19200 Bd-->  node 1 (bridge)  --CAN 62.5 kb/s-->  nodes 2..n
```

- Every node is the same [generic PIC board](../../pcb/pic-board/pic-board.md):
  PIC18F26K80, MCP2551 CAN transceiver, three 8-pin device connectors
  (`PORT1`..`PORT3`, 6 I/O pins each) and a CAN connector that also powers the board.
- Node 1 is an ordinary board with the
  [USB-CAN bridge](../../pcb/usb-bridge/usb-bridge.md) (CH340E USB/TTL module
  and a packet LED) plugged into `PORT3`. Only `PORT3` carries the PIC UART pins.
- The serial port is `system.bridge.port` in `cfg/app.properties`; the baud
  rate is fixed in `Main` (`new PacketUartIO(port, 19200)`).

## Firmware (`pic/node`)

- MPLAB X project (`nbproject/`), XC8 compiler, internal RC oscillator, 1 MHz
  by default.
- One source for all nodes. The node ID is compiled in: `#define NODE_ID` in
  `system.h`, so each node gets its own build. The `NodeId` enum lists only
  1..26, but nodes with higher IDs exist (see the configurators); the enum
  enforces nothing.
- Main loop (`main.c`): read a packet from the UART queue, otherwise from the
  CAN FIFO; process it if `nodeId` matches; send the heartbeat / reboot
  message when the timer fires; run the software PWM; check inputs every 4th
  iteration. The NOPs keep both branches the same length for the software PWM.
- Bridge behaviour is just `if (nodeId == NODE_ROUTER)` branches: it sets up
  the UART, handles packets addressed to node 1 itself, forwards everything
  else UART -> CAN and CAN -> UART, sends its own messages straight to the UART
  and blinks the packet LED on RB1 (active low) on forwarded packets.
- Start-up is staggered by `2000 * (nodeId - 1)` NOPs so the nodes do not boot
  in the same instant.

## Protocol

A packet is `nodeId, messageType, 0..5 data bytes`. `nodeId` is the target of
a request and the source of a response or event; a node always answers with
its own ID. Message types are in `Constants.h` and mirrored in
`MessageType.java`; the order of `MSG_SetPortA..D` and
`MSG_OnPortXPinChange` is relied on by the Java side.

### UART framing (`uart.c` <-> `PacketSerializer`)

- Every byte except the last has bit 7 set; the last byte (bit 7 clear) is the
  CRC and ends the frame.
- The original bit 7 of each byte travels in the last-but-one byte
  ("eighth bits"), bit 0 for the first byte.
- CRC = `length + all bytes + eighth bits`, low 7 bits.
- At most 9 bytes per frame. The PIC receives into a 32-byte ring buffer in
  the ISR.

### CAN mapping (`can_readPacket` / `can_sendPacket`)

- Standard ID with `SIDH = nodeId`, `D0 = messageType`, `D1..D5 = data`,
  `DLC = length - 1`.
- 62.5 kb/s at 1 MHz. `MSG_SetFrequencyRequest` changes the CPU clock and the
  CAN prescaler together so the bus rate stays the same.
- Enhanced FIFO mode with 8 receive buffers and one accept-all filter: every
  node sees all traffic and filters by `nodeId` in software.

## Node lifecycle

1. After reset a node sends `MSG_OnReboot` (heartbeat counter, `RCON`) every
   10 s until it is initialized.
2. `Node.packetReceivedImpl` calls the `onReboot` listeners;
   `NodeListener.onReboot` runs `Node.initialize()`: heartbeat period 60 s,
   `SetPort` with value, event mask and TRIS for each used port, and
   `SetFrequency` if a device needs it. Then `MSG_InitializationFinished` is sent.
3. From then on the node sends `MSG_OnHeartBeat` and, for pins in the event
   mask, `MSG_OnPortXPinChange` (changed-bits mask + values). Debounce is
   `PIN_CHANGE_LOOP_COUNT` (37) main-loop passes per pin.
4. `NodeInfoRegistry` registers every node seen in a sent or received packet,
   records boot and last-ping time, and asks for the build time
   (`MSG_GetBuildTimeRequest`) on the first heartbeat.
5. Diagnostics: `ReadRam` / `WriteRam`, `ReadProgram`, `Echo`, `Reset`.

## Connectors and reserved pins

`AbstractConnectedDevice.layouts` maps connector pins to PIC pins:

| Connector | PIC pins |
| --- | --- |
| 1 | A5, A3, A2, A0, B4, B5 |
| 2 | C3, C1, C0, A6, C2, A7 |
| 3 | B0, B1, C5, C4, C6, C7 |

Some pins are driven by the firmware itself. `Node` knows them
(`getReservedOutputMask()` / `getReservedInputMask()`): `addDevice` rejects a
device that uses them (`DeviceUsesReservedPins`) and `doInitialization` keeps
their TRIS whatever the devices ask for.

| Node | Outputs | Inputs |
| --- | --- | --- |
| every node | RB2 (CAN TX) | RB3 (CAN RX) |
| bridge (node 1), additionally | RC6 (UART TX), RB1 (packet LED) | RC7 (UART RX) |

So devices can be attached to connectors 1 and 2 of the bridge, never to
connector 3. Without this guard a device on port C would set TRISC6 = 1 and
the bridge would stop transmitting to the Pi. No firmware change is needed:
the firmware writes whatever TRIS the controller sends.

On the bridge, keep to inputs and slow outputs (buttons, PIRs, relays). Its
main loop does the forwarding too, so the software PWM would flicker under
bus traffic and the debounce (counted in loop passes) gets longer.

## Caveats

- `Pic.java` holds fixed RAM addresses of firmware variables taken from a
  linker map. They move whenever the firmware is rebuilt, and some
  (`displaySegments`, `displayValue`) no longer exist, so treat
  `readMemory` / `dumpMemory` output with care.
- `can_sendPacket` busy-waits on `TXREQ` with no timeout. If no node
  acknowledges a frame (broken bus, all nodes unpowered) the bridge hangs and
  stops serving the UART as well.
- The bridge has a single UART send buffer and 19200 Bd (~4.7 ms per 9-byte
  frame) is slower than the CAN bus. A burst of more than ~8 CAN frames can
  overflow the CAN FIFO.
- The protocol and `Pin` know ports A..D, but the 28-pin PIC18F26K80 has no
  PORTD; `SetPortD` would write to an address that is not PORTD.
- A misconfigured bridge cuts off the whole bus, and `MSG_ResetRequest` cannot
  reach it any more; only a physical reset or power cycle helps.
