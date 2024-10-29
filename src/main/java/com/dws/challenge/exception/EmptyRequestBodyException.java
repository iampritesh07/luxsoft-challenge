package com.dws.challenge.exception;

public class EmptyRequestBodyException extends RuntimeException{
        public EmptyRequestBodyException(String message) {
            super(message);
        }
}
