package lingfly.proto.messages;

import lingfly.proto.Header;
import lingfly.proto.Message;
import lingfly.proto.encoding.Decoding;
import lingfly.proto.mqtt.Qos;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;

public class Connect implements Message {
    private Logger log = Logger.getLogger(this.getClass().getName());
    Header header;
    String protocolName;
    int protocalVersion;
    boolean willRetain;
    boolean willFlag;
    boolean cleanSession;
    int willQos;
    int keepAliveTimer;
    String clientId;
    String willTopic,willMessage;
    boolean usernameFlag, passwordFlag;
    String username, password;

    @Override
    public void encode(OutputStream w) {
        //CONNECT 为客户端到服务端的报文，可不实现编码
    }

    @Override
    public void decode(InputStream r, Header hdr, int packetRemaining)  {
        log.info("CONNECT报文开始解码");
        header = hdr;
        try {
            //协议名
            protocolName = Decoding.getString(r,packetRemaining);
            packetRemaining=packetRemaining-2-protocolName.getBytes().length;

            //协议级别
            protocalVersion = Decoding.getUint8(r,packetRemaining);
            packetRemaining--;

            //连接标志
            int flags= Decoding.getUint8(r,packetRemaining);
            packetRemaining--;

            usernameFlag = (flags&0x80) > 0;     //7:用户名标志 User Name Flag
            passwordFlag = (flags&0x40) > 0;     //6:密码标志 Password Flag
            willRetain = (flags&0x20) > 0;       //5:遗嘱保留 Will Retain
            willQos = (flags&0x18)>>3;           //3,4:遗嘱QoS Will QoS
            willFlag = (flags&0x04)>0;           //2:遗嘱标志 Will Flag
            cleanSession = (flags&0x02)>0;       //1:清理会话 Clean Session

            //保持连接
            keepAliveTimer = Decoding.getUint16(r,packetRemaining);
            packetRemaining-=2;

            //有效载荷
            //CONNECT报文的有效载荷（payload）包含一个或多个以长度为前缀的字段，可变报头中的
            //标志决定是否包含这些字段。如果包含的话，必须按这个顺序出现：客户端标识符，遗嘱主
            //题，遗嘱消息，用户名，密码 [MQTT-3.1.3-1]。
            clientId = Decoding.getString(r,packetRemaining);
            packetRemaining=packetRemaining-2-clientId.getBytes().length;

            if (willFlag){
                willTopic = Decoding.getString(r,packetRemaining);
                packetRemaining = packetRemaining-2-willTopic.getBytes().length;
                willMessage = Decoding.getString(r,packetRemaining);
                packetRemaining = packetRemaining-2-willMessage.getBytes().length;
            }
            if (usernameFlag){
                username = Decoding.getString(r,packetRemaining);
                packetRemaining = packetRemaining-2-username.getBytes().length;
            }
            if (passwordFlag){
                password = Decoding.getString(r,packetRemaining);
                packetRemaining = packetRemaining-2-password.getBytes().length;
            }
            if (packetRemaining != 0){
                //TODO
                log.severe("CONNECT报文解码Error:剩余长度不为0");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        log.info("CONNECT报文解码完成");
    }

    public Header getHeader() {
        return header;
    }

    public void setHeader(Header header) {
        this.header = header;
    }

    public String getProtocolName() {
        return protocolName;
    }

    public void setProtocolName(String protocolName) {
        this.protocolName = protocolName;
    }

    public int getProtocalVersion() {
        return protocalVersion;
    }

    public void setProtocalVersion(int protocalVersion) {
        this.protocalVersion = protocalVersion;
    }

    public boolean isWillRetain() {
        return willRetain;
    }

    public void setWillRetain(boolean willRetain) {
        this.willRetain = willRetain;
    }

    public boolean isWillFlag() {
        return willFlag;
    }

    public void setWillFlag(boolean willFlag) {
        this.willFlag = willFlag;
    }

    public boolean isCleanSession() {
        return cleanSession;
    }

    public void setCleanSession(boolean cleanSession) {
        this.cleanSession = cleanSession;
    }

    public int getWillQos() {
        return willQos;
    }

    public void setWillQos(int willQos) {
        this.willQos = willQos;
    }

    public int getKeepAliveTimer() {
        return keepAliveTimer;
    }

    public void setKeepAliveTimer(int keepAliveTimer) {
        this.keepAliveTimer = keepAliveTimer;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getWillTopic() {
        return willTopic;
    }

    public void setWillTopic(String willTopic) {
        this.willTopic = willTopic;
    }

    public String getWillMessage() {
        return willMessage;
    }

    public void setWillMessage(String willMessage) {
        this.willMessage = willMessage;
    }

    public boolean isUsernameFlag() {
        return usernameFlag;
    }

    public void setUsernameFlag(boolean usernameFlag) {
        this.usernameFlag = usernameFlag;
    }

    public boolean isPasswordFlag() {
        return passwordFlag;
    }

    public void setPasswordFlag(boolean passwordFlag) {
        this.passwordFlag = passwordFlag;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
