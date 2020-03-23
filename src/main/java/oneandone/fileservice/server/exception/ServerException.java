package oneandone.fileservice.server.exception;

public class ServerException extends ServiceException {

    public ServerException(String message, Throwable e) {
        super(ClientExceptionMessage.GENERAL_ERROR, message, e);
    }

    public ServerException(String s) {
        super(ClientExceptionMessage.GENERAL_ERROR,s);
    }
}
