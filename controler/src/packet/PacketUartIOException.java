package packet;

public class PacketUartIOException extends Exception {
    public PacketUartIOException(Exception e) {
        super(e);
    }
    public PacketUartIOException(String message, Exception e) {
        super(message, e);
    }
}