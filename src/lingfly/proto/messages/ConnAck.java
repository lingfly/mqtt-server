package lingfly.proto.messages;

import lingfly.proto.Header;
import lingfly.proto.Message;
import lingfly.proto.encoding.Encoding;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class ConnAck implements Message {
    Header header;
    int returnCode;

    @Override
    public void encode(OutputStream w) {


        ByteBuffer buf = ByteBuffer.allocate(Message.MaxPayloadSize);
        buf.put((byte) 0);
        buf.put((byte) returnCode);
        try {
            Encoding.writeMessage(w,Message.MsgConnAck,header,buf,0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void decode(InputStream r, Header hdr, int packetRemaining) {

    }

    public ConnAck() {
        header = new Header();
    }

    public void setReturnCode(int returnCode) {
        this.returnCode = returnCode;
    }
}
