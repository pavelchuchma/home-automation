# Wall Switch Board

It has 4 micro switches in corners, which may be pressed independently. It may have one bi-color led for signaling.

Exact PCB board size and micro switch height needs to be tuned for specific switch type. 
I'm using it with [ABB Time series](https://nizke-napeti.cz.abb.com/designy/time) with 8mm high micro switches on 2mm thick PCB board.

Pins of FC-8P connector to be connected to [pic-board](..%2Fpic-board%2Fpic-board.md). Up to three wall-switches can be
connected to one pic-board.

This board is represented by Java class `WallSwitch`

## Parts
 * 1x resistor array RRA 4X10k
 * 4x micro switch 6*6*8 mm (SMD)
 * 1x resistor R180
 * 1x bi-color led
 * 1x flat cable with FC-8P connector
 
## Sources

* FreePCB 2.0 sources are in `FreePCB` folder
* PCB layout as PDF: [141209-wall-switch.pdf](FreePCB%2Fpdf%2F141209-wall-switch.pdf)
* OpenSCAD source of pcb pad. May be used for tuning micro switch height: [abb-time-pad.scad](pad%2Fabb-time-pad.scad)

## Pin layout

FC-8P connector (pin 1 is on the left side of schema)
```
1 - VDD out
2 - VSS
3 - SW 1
4 - SW 2
5 - SW 3
6 - SW 4
7 - Green LED
8 - Red LED
```

## Photos

![schema.png](img%2Fschema.png)

![photo1.jpg](img%2Fphoto1.jpg) 

![photo2.jpg](img%2Fphoto2.jpg)