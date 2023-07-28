package com.kg.mrw.tracking.telegram.exception;

public class BotException extends RuntimeException{
    public BotException(String message){
        super(message);
    }
}
