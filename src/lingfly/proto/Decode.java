package lingfly.proto;

import lingfly.proto.messages.*;

import java.io.IOException;
import java.io.InputStream;

public class Decode {

    public static Message decodeOneMessage(InputStream r) throws IOException {
        Header hdr = new Header();
        int msgType;
        int packetRemaining;
        //解析固定报头
        MessageResult messageResult = hdr.decode(r);
        msgType = messageResult.msgType;
        packetRemaining = messageResult.packetRemaining;

        //根据报文类型解析报文
        Message msg = newMessage(msgType);
        if (msg == null){
            //TODO
            return null;
        }
        msg.decode(r, hdr, packetRemaining);
        return msg;
    }
    private static Message newMessage(int msgType){
        Message msg;
        switch (msgType){
            case Message.MsgConnect:
                msg = new Connect();
                break;
            case Message.MsgConnAck:
                msg = new ConnAck();
                break;
            case Message.MsgPublish:
                msg = new Publish();
                break;
            case Message.MsgPubAck:
                msg = new PubAck();
                break;
            case Message.MsgSubscribe:
                msg = new Subscribe();
                break;

            case Message.MsgUnsubscribe:
                msg = new Unsubscribe();
                break;
            case Message.MsgPingReq:
                msg = new PingReq();
                break;
            case Message.MsgDisconnect:
                msg = new Disconnect();
                break;
            default:
                msg = null;
                break;
        }
        return msg;
    }
}
