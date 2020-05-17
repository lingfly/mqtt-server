package lingfly.proto;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface Payload {
    int size();
    void writePayload(OutputStream w) throws IOException;
    void readPayload(InputStream r)  throws IOException;
}
