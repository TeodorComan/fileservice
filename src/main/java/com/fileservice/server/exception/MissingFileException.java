package com.fileservice.server.exception;

public class MissingFileException extends RuntimeException{

    public MissingFileException( String message) {
        super(message);
    }
}
