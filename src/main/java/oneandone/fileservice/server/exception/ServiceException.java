package oneandone.fileservice.server.exception;

public abstract class ServiceException extends RuntimeException {

    private ClientExceptionMessage clientExceptionMessage;


    public ServiceException(ClientExceptionMessage clientExceptionMessage, String message, Throwable e) {
        super(message,e);
        this.clientExceptionMessage = clientExceptionMessage;
    }

    public ServiceException(ClientExceptionMessage clientExceptionMessage, String message) {
        super(message);
        this.clientExceptionMessage = clientExceptionMessage;
    }

    public ClientExceptionMessage getClientExceptionMessage() {
        return clientExceptionMessage;
    }

}
