# PIC Board

Generic board with PIC and CAN-BUS connector representing "node" in the application. 

It has 18 generic IO pins in 3 ports. It is powered from VDD pin of CAN port. 

## Parts
 * 1x Self-Recovery Fuse `PPTC1812SMD020/30V`
   * PolySwitch (PTC) 0,2A/30V, case 1812
 * 2x Ceramic capacitor `CK 100n/63V Y5V RM5`
 * 1x SMD Resistor `R0805 10k`
 * 1x SMD Resistor `R0805 R330`
 * 1x 28 DIP socket
 * 1x 8 DIP socket
 * 1x `PIC18F26K80-E/SP` or `PIC18F26K80-I/SP`
 * 1x High-Speed CAN Transceiver `MCP2551-I/P`
 * 2x30 Double Row Male Pin Header Connector Strip

## Sources

* FreePCB 2.0 sources are in `FreePCB` folder
* PCB layout as PDF: [130521-pic-board.pdf](FreePCB%2Fpdf%2F130521-pic-board.pdf)

## Pin layout

### Port #1
```
1 - VDD out
2 - VSS
3 - PIC.7 - RA5/AN4/C2INB/HLVDIN/T1CKI/SS/CTMUI
4 - PIC.5 - RA3/VREF+/AN3
5 - PIC.4 - RA2/VREF-/AN2
6 - PIC.2 - RA0/CVREF/AN0/ULPWU
7 - PIC.25 - RB4/AN9/C2INA/ECCP1/P1A/CTPLS/KBI0
8 - PIC.26 - RB5/T0CKI/T3CKI/CCP5/KBI1
```

### Port #2
```
1 - VDD out
2 - VSS
3 - PIC.14 - RC3/REFO/SCL/SCK
4 - PIC.12 - RC1/ISOSCI
5 - PIC.11 - RC0/SOSCO/SCLKI
6 - PIC.10 - RA6/OSC2/CLKOUT
7 - PIC.13 - RC2/T1G/CCP2
8 - PIC.9 - RA7/OSC1/CLKIN
```

### Port #3
```
1 - VDD out
2 - VSS
3 - PIC.21 - RB0/AN10/C1INA/FLT0/INT0
4 - PIC.22 - RB1/AN8/C1INB/P1B/CTDIN/INT1
5 - PIC.16 - RC5/SDO
6 - PIC.15  - RC4/SDA/SDI
7 - PIC.17 - RC6/CANTX/TX1/CK1/CCP3
8 - PIC.18 - RC7/CANRX/RX1/DT1/CCP4
```
### CAN
```
1 - VDD in (+5V, fused)
2 - VSS
3 - CAN-H
4 - CAN-L
5 - NC
6 - NC
```
## Photos

![schema-top.png](img%2Fschema-top.png) ![schema-bottom.png](img%2Fschema-bottom.png)
![photo-top.jpg](img%2Fphoto-top.jpg) ![photo-bottom.jpg](img%2Fphoto-bottom.jpg)

