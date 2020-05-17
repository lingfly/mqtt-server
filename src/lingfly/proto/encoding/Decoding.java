package lingfly.proto.encoding;

import java.io.IOException;
import java.io.InputStream;

public class Decoding {
    public static byte boolToByte(boolean val){
        byte b = 0;
        if (val){
            b = 1;
            return b;
        }
        return b;
    }

    public static String getString(InputStream r, int packetRemaining) throws IOException {
        int strLen = getUint16(r,packetRemaining);
        if (packetRemaining<strLen){
            //TODO
        }
        byte[] b = new byte[strLen];
        r.read(b);
        return new String(b);
    }
    public static int getUint8(InputStream r, int packetRemaining) throws IOException {
        if (packetRemaining < 2){
            //TODO:
        }
        byte[] b = new byte[1];
        r.read(b);
        //packetRemaining--;
        return b[0];
    }
    public static int getUint16(InputStream r,int packetRemaining) throws IOException {
        if (packetRemaining < 2){
            //TODO:
        }
        byte[] b = new byte[2];
        r.read(b);
        //packetRemaining-=2;
        return (b[0]&0x00ff)<<8 | (b[1]&0x00ff);
    }
    public static int decodeAckCommon(InputStream r, int packetRemaining) throws IOException {
        int messageId = Decoding.getUint16(r,packetRemaining);
        packetRemaining = packetRemaining - 2;
        if (packetRemaining != 0){
            System.out.println("error:剩余长度不为0");
        }
        return messageId;
    }
}
