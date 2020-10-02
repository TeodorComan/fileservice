package com.fileservice.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.fileservice.server.exception.ClientException;
import com.fileservice.server.exception.ClientExceptionMessage;
import com.fileservice.server.exception.ServerException;

@org.springframework.web.bind.annotation.ControllerAdvice
public class ControllerAdvice {

    @ExceptionHandler(value = {ClientException.class})
    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String processBadRequestException(ClientException e) {
        return e.getClientExceptionMessage().toString();
    }

    @ExceptionHandler(value = {ServerException.class,Exception.class})
    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String processInternalErrorExceptions() {
        return ClientExceptionMessage.GENERAL_ERROR.toString();
    }
}
