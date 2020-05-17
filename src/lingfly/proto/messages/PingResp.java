package lingfly.proto.messages;

import lingfly.proto.Header;
import lingfly.proto.Message;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PingResp implements Message {
    Header header;
    @Override
    public void encode(OutputStream w) throws IOException {
        header.encode(w,Message.MsgPingResp,0);
    }

    @Override
    public void decode(InputStream r, Header hdr, int packetRemaining) throws IOException {
        header = hdr;
        if (packetRemaining!=0){
            //TODO:
        }
    }

    public PingResp() {
        header = new Header();
    }
}
