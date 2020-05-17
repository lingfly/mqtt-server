package lingfly.proto;

import lingfly.proto.encoding.Decoding;
import lingfly.proto.encoding.Encoding;
import lingfly.proto.mqtt.Qos;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class Header{
    boolean DupFlag;
    boolean Retain;
    int QosLevel;

    public MessageResult decode(InputStream r) throws IOException {
        int msgType;
        int remainingLength;
        //取第一个字节
        byte[] buf = new byte[1];
        r.read(buf);
        byte byte1 = buf[0];
        //高4位表示报文类型
        msgType = (byte1 & 0xF0) >> 4;
        //剩余4位表示标志位或者保留
        this.DupFlag = (byte1 & 0x08)>0 ? true:false;
        this.QosLevel = (byte1 & 0x06) >> 1;
        this.Retain = (byte1 & 0x01)>0 ? true:false;
        //剩余长度
        remainingLength = decodeLength(r);
        return new MessageResult(msgType, remainingLength);
    }
    int decodeLength(InputStream r){
        int v = 0;
        byte[] buf = new byte[1];
        int shift = 0;
        for (int i = 0; i < 4; i++){
            try {
                r.read(buf);
                byte b = buf[0];
                v|=(b&0x7F)<<shift;
                if ((b&0x80) == 0){
                    return v;
                }
                shift += 7;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //TODO:剩余长度校验
        return v;
    }
    public void encode(OutputStream w,int msgType,int remainingLength) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(Message.MaxPayloadSize);
        encodeInto(buf,msgType,remainingLength);
        byte[] dst = new byte[buf.position()];
        buf.flip();
        buf.get(dst);
        w.write(dst);
    }
    public void encodeInto(ByteBuffer buf, int msgType, int remainingLength){
        if (Qos.isValid(QosLevel)){

        }
        if (Encoding.isValid(msgType)){

        }
        byte val = (byte)(msgType<<4);
        val |= (byte)(Decoding.boolToByte(DupFlag)<<3);
        val |= (byte)(QosLevel<<1);
        val |= (byte)(Decoding.boolToByte(Retain));
        buf.put(val);
        encodeLength(remainingLength,buf);
    }
    void encodeLength(int length,ByteBuffer buf){
        if (length == 0){
            buf.put((byte) 0);
        }
        for (;length > 0;){
            byte digit = (byte) (length & 0x7f);
            length = length >> 7;
            if (length > 0){
                digit = (byte) (digit |0x80);
            }
            buf.put(digit);
        }
    }

    public boolean isDupFlag() {
        return DupFlag;
    }

    public void setDupFlag(boolean dupFlag) {
        DupFlag = dupFlag;
    }

    public boolean isRetain() {
        return Retain;
    }

    public void setRetain(boolean retain) {
        Retain = retain;
    }

    public int getQosLevel() {
        return QosLevel;
    }

    public void setQosLevel(short qosLevel) {
        QosLevel = qosLevel;
    }
}
