package lingfly.proto.constant;

public interface MessageType {
     String CONNECT = "lingfly.proto.messages.Connect";
     String CONNACK = "lingfly.proto.messages.ConnAck";
     String PUBLISH = "lingfly.proto.messages.Publish";
     String PUBACK = "lingfly.proto.messages.PubAck";
     String SUBSCRIBE = "lingfly.proto.messages.Subscribe";
     String SUBACK = "lingfly.proto.messages.SubAck";
     String UNSUBSCRIBE = "lingfly.proto.messages.Unsubscribe";
     String UNSUBACK = "lingfly.proto.messages.UnsubAck";
     String PINGREQ = "lingfly.proto.messages.PingReq";
     String PINGRESP = "lingfly.proto.messages.PingResp";
     String DISCONNECT = "lingfly.proto.messages.Disconnect";
}
