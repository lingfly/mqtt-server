package lingfly.proto.messages;

import lingfly.proto.Header;
import lingfly.proto.Message;
import lingfly.proto.encoding.Decoding;
import lingfly.proto.encoding.Encoding;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PubRel implements Message {
    Header header;
    int messageId;
    @Override
    public void encode(OutputStream w) throws IOException {
        Encoding.encodeAckCommon(w,header,messageId,Message.MsgPubRel);
    }

    @Override
    public void decode(InputStream r, Header hdr, int packetRemaining) throws IOException {
        header = hdr;
        messageId = Decoding.decodeAckCommon(r,packetRemaining);
    }
}
