package lingfly.proto;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BytesPayload implements Payload{
    byte[] p;
    public static BytesPayload newBytesPayload(int n){
        return new BytesPayload(n);
    }
    @Override
    public int size() {
        return p.length;
    }

    @Override
    public void writePayload(OutputStream w) throws IOException {
        w.write(p);
    }

    @Override
    public void readPayload(InputStream r) throws IOException {
        r.read(p);
    }

    private BytesPayload(int n) {
        this.p = new byte[n];
    }
}
