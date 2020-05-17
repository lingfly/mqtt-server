package lingfly.proto.messages;

import lingfly.proto.Header;
import lingfly.proto.Message;
import lingfly.proto.encoding.Decoding;
import lingfly.proto.encoding.Encoding;
import lingfly.proto.mqtt.Qos;
import lingfly.proto.mqtt.TopicQos;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class Unsubscribe implements Message {
    Header header;
    int messageId;
    List<String> topics;
    @Override
    public void encode(OutputStream w) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(Message.MaxPayloadSize);
        if (Qos.hasId(header.getQosLevel())){
            Encoding.setUint16(messageId,buf);
        }
        for (String topic : topics){
            Encoding.setString(topic,buf);
        }
        Encoding.writeMessage(w,Message.MsgUnsubscribe,header,buf,0);
    }

    @Override
    public void decode(InputStream r, Header hdr, int packetRemaining) throws IOException {
        System.out.println("Unsubscribe报文开始解码");
        header = hdr;
        int qos = header.getQosLevel();
        if (qos == Qos.QosAtLeastOnce || qos == Qos.QosExactlyOnce){
            messageId = Decoding.getUint16(r,packetRemaining);
            packetRemaining = packetRemaining - 2;
        }
        topics = new ArrayList<>();
        while (packetRemaining > 0){
            String topic = Decoding.getString(r,packetRemaining);
            topics.add(topic);
            packetRemaining = packetRemaining - topic.getBytes().length - 2;
        }
        System.out.println("Unsubscribe报文解码完成");
    }

    public Unsubscribe() {
        header = new Header();
    }

    public Header getHeader() {
        return header;
    }

    public int getMessageId() {
        return messageId;
    }

    public List<String> getTopics() {
        return topics;
    }
}
