# USB - CANBUS bridge
Connects any computer (PC or raspberry-pi) to the bridge node via USB port.

It is designed to fit on PORT3 of [generic pic board](..%2Fpic-board%2Fpic-board.md). 
It cannot be used on any other port, because only pins of PORT3 are connected to PIC RX/TX pins.  

## Parts
 * [CH340E USB to TTL Module](https://www.aliexpress.com/item/1005002967540364.html?spm=a2g0o.order_list.order_list_main.26.2f2b1802dCyLXi)
 * [FC-10P](https://www.aliexpress.com/item/32956962437.html?spm=a2g0o.order_list.order_list_main.5.2f2b1802dCyLXi)
 * 1x LED
 * 1x resistor 820R

## 3D printer sources
 * [usb-bridge.scad](socket%2Fusb-bridge.scad) - OpenSCAD sources of the socket

## Photos
![photo1.jpg](img%2Fphoto1.jpg) ![photo2.jpg](img%2Fphoto2.jpg)

![CH340E.jpg](img%2FCH340E.jpg)

![PicBoardPort3ToCh340e.jpg](img%2FPicBoardPort3ToCh340e.jpg)

