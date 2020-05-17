package lingfly.proto.messages;

import lingfly.proto.Header;
import lingfly.proto.Message;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Disconnect implements Message {
    Header header;

    public Disconnect() {
        this.header = new Header();
    }

    @Override
    public void encode(OutputStream w) {
        try {
            header.encode(w,Message.MsgDisconnect,0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void decode(InputStream r, Header hdr, int packetRemaining) {
        if (packetRemaining != 0){//Disconnect报文没有有效负载

        }
    }
}
