package com.dws.challenge.exception;

import com.dws.challenge.dto.ResponseWrapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ResponseWrapper<Object>> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {

        ResponseWrapper<Object> response = new ResponseWrapper<>(
                null,
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value()
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class) //For invalid args line negative balance
    public ResponseEntity<ResponseWrapper<Object>> handleInvalidArgumentsException(MethodArgumentNotValidException ex, WebRequest request){
        ResponseWrapper<Object> response = new ResponseWrapper<>(
                null,
                "Please check the input arguments " + ex.getMessage(),
                HttpStatus.BAD_REQUEST.value()
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class) //For blank requests or other type of requests
    public ResponseEntity<ResponseWrapper<Object>> handleMediaTypeException(HttpMediaTypeNotSupportedException ex, WebRequest request){
        ResponseWrapper<Object> response = new ResponseWrapper<>(
                null,
                "Please ensure to send full body of the input with allowed values " + ex.getMessage(),
                HttpStatus.BAD_REQUEST.value()
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
    @ExceptionHandler(EmptyRequestBodyException.class)
    public ResponseEntity<ResponseWrapper<Object>> handleEmptyRequestBodyException(
            EmptyRequestBodyException ex, WebRequest request) {

        ResponseWrapper<Object> response = new ResponseWrapper<>(
                null,
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value()
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(TransferException.class)
    public ResponseEntity<ResponseWrapper<Object>> handlerTransferExceptions(
            TransferException ex, WebRequest request) {

      /*  if(ex.getMessage().contains("Insufficient Balance")){
            ResponseWrapper<Object> response = new ResponseWrapper<>(
                    null,
                    ex.getMessage(),
                    HttpStatus.BAD_REQUEST.value()
            );

            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }*/

        ResponseWrapper<Object> response = new ResponseWrapper<>(
                null,
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value()
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseWrapper<Object>> handleGeneralException(
            Exception ex, WebRequest request) {

        ResponseWrapper<Object> response = new ResponseWrapper<>(
                null,
                "An unexpected error occurred: " + ex.getClass(),
                HttpStatus.INTERNAL_SERVER_ERROR.value()
        );

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}