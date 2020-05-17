package lingfly.proto.messages;

import lingfly.proto.Header;
import lingfly.proto.Message;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;

public class PingReq implements Message {
    private Logger log = Logger.getLogger(this.getClass().getName());
    Header header;
    @Override
    public void encode(OutputStream w) throws IOException {
        header.encode(w,Message.MsgPingReq,0);
    }

    @Override
    public void decode(InputStream r, Header hdr, int packetRemaining) throws IOException {
        log.info("PINGREQ报文开始解码");
        header = hdr;
        if (packetRemaining!=0){
            //TODO:
        }
        log.info("PINGREQ报文解码完成");
    }
}
