package serial.poc;

import java.io.FileWriter;

import org.junit.Test;

public class PacketDataPrinterTest {
    @Test
    public void testFormatFile() throws Exception {
//        PacketFileReader reader = new PacketFileReader("C:\\Home\\chuma\\work\\HomeAutomation\\SerialPortTest\\data\\171105\\tmp\\raw-20171105_08-59-27.log");
//        PacketFileSource reader = new PacketFileSource("C:\\Home\\chuma\\work\\HomeAutomation\\SerialPortTest\\data\\171108\\raw-20171108_11-43-44.log");
//        PacketFileSource reader = new PacketFileSource("C:\\Home\\chuma\\work\\HomeAutomation\\SerialPortTest\\data\\171112\\raw-20171112_09-54-35.log");
//        PacketFileSource reader = new PacketFileSource("C:\\Home\\chuma\\work\\HomeAutomation\\SerialPortTest\\data\\raw-20171113_09-19-54.log");
//        PacketFileSource reader = new PacketFileSource("C:\\Home\\chuma\\work\\HomeAutomation\\SerialPortTest\\data\\raw-20171113_09-42-07.log");
        PacketFileSource reader = new PacketFileSource("C:\\Home\\chuma\\work\\HomeAutomation\\SerialPortTest\\data\\raw-20171113_10-03-37.log");

        IPacketConsumer consumer = new PacketPrinter(new HtmlOutputWriter(new FileWriter("C:\\Home\\chuma\\work\\HomeAutomation\\SerialPortTest\\data\\171105\\tmp\\out.html")));
        consumer.consume(reader);
    }
}