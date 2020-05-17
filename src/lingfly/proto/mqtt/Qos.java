package lingfly.proto.mqtt;

public class Qos {
    public final static int QosAtMostOnce=0;
    public final static int QosAtLeastOnce=1;
    public final static int QosExactlyOnce=2;
    public final static int qosFirstInvalid=3;

    public static boolean isValid(int qos){
        return qos < qosFirstInvalid;
    }
    public static boolean hasId(int qos){
        return qos == QosAtLeastOnce || qos == QosExactlyOnce;
    }
}
