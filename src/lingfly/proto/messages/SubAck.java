package lingfly.proto.messages;

import lingfly.proto.Header;
import lingfly.proto.Message;
import lingfly.proto.encoding.Decoding;
import lingfly.proto.encoding.Encoding;
import lingfly.proto.mqtt.TopicQos;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class SubAck implements Message {
    Header header;
    int messageId;
    List<Integer> topicsQos;
    @Override
    public void encode(OutputStream w) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(Message.MaxPayloadSize);
        Encoding.setUint16(messageId,buf);
        for (int i=0; i<topicsQos.size(); i++){
            Encoding.setUint8(topicsQos.get(i),buf);
        }
        Encoding.writeMessage(w,Message.MsgSubAck,header,buf,0);
    }

    @Override
    public void decode(InputStream r, Header hdr, int packetRemaining) throws IOException {
        header = hdr;
        while (packetRemaining > 0){
            int grantedQos = Decoding.getUint8(r, packetRemaining) & 0x03;
            packetRemaining = packetRemaining - 1;
            topicsQos.add(grantedQos);
        }
    }

    public SubAck() {
        header = new Header();
        topicsQos = new ArrayList<>();
    }

    public SubAck(int messageId) {
        header = new Header();
        this.messageId = messageId;
        topicsQos = new ArrayList<>();
    }

    public List<Integer> getTopicsQos() {
        return topicsQos;
    }

    public void setTopicsQos(List<Integer> topicsQos) {
        this.topicsQos = topicsQos;
    }
}
