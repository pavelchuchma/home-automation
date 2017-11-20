package serial.poc;

import java.io.IOException;

public interface IPacketConsumer {
    void consume(IPacketSource reader) throws IOException, InterruptedException;
}
