package com.como.epoxy.controller;

import com.como.epoxy.Exception.RequestFailedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.concurrent.TimeoutException;

@RestControllerAdvice
public class EpoxyControllerAdvice {

    @ExceptionHandler(TimeoutException.class)
    public ResponseEntity<String> handleTimeoutException(TimeoutException timeoutException) {
        return new ResponseEntity<>("Not able fulfil the requirement within requested time period", HttpStatus.REQUEST_TIMEOUT);
    }

    @ExceptionHandler(RequestFailedException.class)
    public ResponseEntity<String> handleRequestFailedException(RequestFailedException timeoutException) {
        return new ResponseEntity<>("Execution failed for URL : " + timeoutException.getMessage(), HttpStatus.BAD_GATEWAY);
    }

}
