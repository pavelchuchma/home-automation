package serial.poc;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.Arrays;

import org.junit.Test;
import serial.poc.Packet.PacketConsumer;
import serial.poc.device.HvacDevice;

public class PacketDataPrinterTest {
    @Test
    public void testFormatFile() throws Exception {
//        PacketFileReader reader = new PacketFileReader("C:\\Home\\chuma\\work\\HomeAutomation\\SerialPortTest\\data\\171105\\tmp\\raw-20171105_08-59-27.log");
//        PacketFileSource reader = new PacketFileSource("C:\\Home\\chuma\\work\\HomeAutomation\\SerialPortTest\\data\\171108\\raw-20171108_11-43-44.log");
//        PacketFileSource reader = new PacketFileSource("C:\\Home\\chuma\\work\\HomeAutomation\\SerialPortTest\\data\\171112\\raw-20171112_09-54-35.log");
//        PacketFileSource reader = new PacketFileSource("C:\\Home\\chuma\\work\\HomeAutomation\\SerialPortTest\\data\\raw-20171113_09-19-54.log");
//        PacketFileSource reader = new PacketFileSource("C:\\Home\\chuma\\work\\HomeAutomation\\SerialPortTest\\data\\raw-20171113_09-42-07.log");
//        PacketFileSource reader = new PacketFileSource("C:\\Home\\chuma\\work\\HomeAutomation\\SerialPortTest\\data\\raw-20171113_10-03-37.log");
//        PacketFileSource reader = new PacketFileSource("C:\\Home\\chuma\\work\\HomeAutomation\\controler\\raw-20180107_08-55-41.log");
//        PacketFileSource reader = new PacketFileSource("C:\\Temp\\raw-20180216_11-30-32.log");
//        PacketFileSource reader = new PacketFileSource("C:\\Home\\chuma\\work\\HomeAutomation\\SerialPortTest\\data\\20180302_07-44-51-defrostEndAtEnd-5\\raw-20180302_07-44-51-defrostEndAtEnd-5.log");
//        PacketFileSource reader = new PacketFileSource("C:\\Home\\chuma\\work\\HomeAutomation\\SerialPortTest\\data\\20180302_12-39-37-TurnOn Maybe with pause at beginning (maybe defrost)\\raw-20180302_12-39-37.f1");
//        PacketFileSource reader = new PacketFileSource("C:\\Home\\chuma\\work\\HomeAutomation\\SerialPortTest\\data\\20180302_10-16-12-TurnOn-InTemp\\raw-20180302_10-16-12.log");
        PacketFileSource reader = new PacketFileSource("C:\\Home\\chuma\\work\\HomeAutomation\\SerialPortTest\\data\\A0-2\\A0.messages");

        IPacketProcessor processor = new PacketPrinter(new HtmlOutputWriter(new FileWriter("C:\\Home\\chuma\\work\\HomeAutomation\\SerialPortTest\\data\\A0-2\\A0.html")));
        HvacDevice hvacDevice = new HvacDevice(null);
        PacketConsumer consumer = new PacketConsumer(Arrays.asList(processor, hvacDevice.getProcessor()));
        consumer.consume(reader);
    }

    @Test
    public void testWriteBytes() throws Exception {
        FileOutputStream os = new FileOutputStream("C:\\temp\\aaa.tmp");
        int[] data = new int[]{0x32, 0x84, 0x20, 0xA0, 0x1F, 0x18, 0x55, 0x04, 0xF4, 0x00, 0x00, 0x00, 0xA6, 0x34};
        for (int c : data) {
            os.write(new byte[]{(byte) c});
        }
        os.close();

    }
}