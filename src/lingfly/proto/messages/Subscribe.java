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
import java.util.logging.Logger;

public class Subscribe implements Message {
    private Logger log = Logger.getLogger(this.getClass().getName());
    Header header;
    int messageId;
    List<TopicQos> topics;

    public Subscribe() {
        header = new Header();
        topics = new ArrayList<>();
    }

    @Override
    public void encode(OutputStream w) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(Message.MaxPayloadSize);
        if (Qos.hasId(header.getQosLevel())){
            Encoding.setUint16(messageId, buf);
        }
        for (TopicQos topicSub : topics){
            Encoding.setString(topicSub.getTopic(),buf);
            Encoding.setUint8(topicSub.getQos(),buf);
        }
        Encoding.writeMessage(w,Message.MsgSubscribe,header,buf,0);
    }

    @Override
    public void decode(InputStream r, Header hdr, int packetRemaining) throws IOException {
        log.info("SUBSCRIBE报文开始解码");
        header = hdr;
        if (Qos.hasId(header.getQosLevel())){
            messageId = Decoding.getUint16(r, packetRemaining);
            packetRemaining = packetRemaining - 2;
        }

        while (packetRemaining > 0){
            String topic = Decoding.getString(r,packetRemaining);
            packetRemaining = packetRemaining - topic.getBytes().length - 2;

            int qos = Decoding.getUint8(r,packetRemaining);
            packetRemaining = packetRemaining - 1;
            topics.add(new TopicQos(topic,qos));
            log.info("SUBSCRIBE报文解码完成");
        }


    }

    public Header getHeader() {
        return header;
    }

    public int getMessageId() {
        return messageId;
    }

    public List<TopicQos> getTopics() {
        return topics;
    }
}
