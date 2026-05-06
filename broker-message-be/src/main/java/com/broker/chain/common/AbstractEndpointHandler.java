package com.broker.chain.common;

public abstract class AbstractEndpointHandler<C> implements EndpointHandler<C> {

    private EndpointHandler<C> next;

    @Override
    public void setNext(EndpointHandler<C> next) {
        this.next = next;
    }

    protected void handleNext(C context) {
        if (next != null) {
            next.handle(context);
        }
    }
}