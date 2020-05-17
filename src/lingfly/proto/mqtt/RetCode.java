package lingfly.proto.mqtt;

public interface RetCode {
    int RetCodeAccepted = 0;
    int RetCodeUnacceptableProtocolVersion = 1;
    int RetCodeIdentifierRejected = 2;
    int RetCodeServerUnavailable = 3;
    int RetCodeBadUsernameOrPassword = 4;
    int RetCodeNotAuthorized = 5;

    int retCodeFirstInvalid = 6;
}
