package com.broker.chain.common;

public interface EndpointHandler<C> {

    void setNext(EndpointHandler<C> next);

    void handle(C context);
}