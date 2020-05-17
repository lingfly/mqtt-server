package lingfly.proto.messages;

import lingfly.proto.BytesPayload;
import lingfly.proto.Header;
import lingfly.proto.Message;
import lingfly.proto.Payload;
import lingfly.proto.encoding.Decoding;
import lingfly.proto.encoding.Encoding;
import lingfly.proto.mqtt.Qos;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

public class Publish implements Message {
    private Logger log = Logger.getLogger(this.getClass().getName());
    Header header;
    String topicName;
    int messageId;
    Payload payload;
    @Override
    public void encode(OutputStream w) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(Message.MaxPayloadSize);
        Encoding.setString(topicName,buf);
        if (Qos.hasId(header.getQosLevel())){
            Encoding.setUint16(messageId,buf);
        }
        Encoding.writeMessage(w,Message.MsgPublish,header,buf,payload.size());
        payload.writePayload(w);
    }

    @Override
    public void decode(InputStream r, Header hdr, int packetRemaining) throws IOException {
        log.info("PUBLISH报文开始解码");
        header = hdr;
        topicName = Decoding.getString(r, packetRemaining);
        packetRemaining = packetRemaining -topicName.getBytes().length - 2;
        if (Qos.hasId(header.getQosLevel())){
            messageId = Decoding.getUint16(r, packetRemaining);
            packetRemaining = packetRemaining - 2;
        }
        payload = BytesPayload.newBytesPayload(packetRemaining);
        payload.readPayload(r);
        log.info("PUBLISH报文解码完成");
    }

    public Header getHeader() {
        return header;
    }

    public String getTopicName() {
        return topicName;
    }

    public int getMessageId() {
        return messageId;
    }

    public Payload getPayload() {
        return payload;
    }
}
