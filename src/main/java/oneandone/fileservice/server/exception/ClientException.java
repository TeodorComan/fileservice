package oneandone.fileservice.server.exception;

public class ClientException extends ServiceException {

    public ClientException(ClientExceptionMessage clientExceptionMessage, String message, Throwable e) {
        super(clientExceptionMessage, message, e);
    }

    public ClientException(ClientExceptionMessage clientExceptionMessage, String message) {
        super(clientExceptionMessage, message);
    }
}
