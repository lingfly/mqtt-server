package lingfly.proto;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface Message {
    int MsgConnect = 1;
    int MsgConnAck = 2;
    int MsgPublish =3;
    int MsgPubAck = 4;
    int MsgPubRec = 5;
    int MsgPubRel = 6;
    int MsgPubComp = 7;
    int MsgSubscribe = 8;
    int MsgSubAck = 9;
    int MsgUnsubscribe = 10;
    int MsgUnsubAck = 11;
    int MsgPingReq = 12;
    int MsgPingResp = 13;
    int MsgDisconnect = 14;
    int msgTypeFirstInvalid = 15;

    // Maximum payload size in bytes (256MiB - 1B).
    int MaxPayloadSize = (1 << (2 * 8)) - 1;

    void encode(OutputStream w) throws IOException;
    void decode(InputStream r,Header hdr,int packetRemaining) throws IOException;
}
