package lingfly.proto;

public class MessageResult {
    int msgType;
    int packetRemaining;

    public MessageResult(int msgType, int packetRemaining) {
        this.msgType = msgType;
        this.packetRemaining = packetRemaining;
    }
}
