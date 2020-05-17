package lingfly.proto.encoding;

import lingfly.proto.Header;
import lingfly.proto.Message;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class Encoding {
    public static void writeMessage(OutputStream w, int msgType, Header hdr, ByteBuffer patloadBuf, int extraLength) throws IOException {
        long totalPayloadLength = (long) patloadBuf.position()+(long)extraLength;
        if (totalPayloadLength > Message.MaxPayloadSize){
            //TODO:
        }
        ByteBuffer buf = ByteBuffer.allocate(Message.MaxPayloadSize);
        hdr.encodeInto(buf,msgType, (int) totalPayloadLength);
        patloadBuf.flip();
        buf.put(patloadBuf);
        byte[] dst = new byte[buf.position()];
        buf.flip();
        buf.get(dst);
        w.write(dst);
    }
    public static boolean isValid(int msgType){
        return msgType >= Message.MsgConnect && msgType < Message.msgTypeFirstInvalid;
    }
    public static void setUint8(int val, ByteBuffer buf){
        buf.put((byte) val);
    }
    public static void setUint16(int val, ByteBuffer buf){
        buf.put((byte) ((val & 0xff00) >> 8));
        buf.put((byte) (val & 0x00ff));
    }
    public static void setString(String val, ByteBuffer buf){
        int length = val.length();
        setUint16(length, buf);
        buf.put(val.getBytes());
    }
    public static void encodeAckCommon(OutputStream w, Header hdr, int messageId,int msgType) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(Message.MaxPayloadSize);
        Encoding.setUint16(messageId,buf);
        Encoding.writeMessage(w,msgType,hdr,buf,0);
    }
}
