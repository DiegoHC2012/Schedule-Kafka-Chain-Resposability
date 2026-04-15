package com.broker.chain;

public interface RetryHandler {

    void setNext(RetryHandler next);

    void handle(RetryContext context);
}
