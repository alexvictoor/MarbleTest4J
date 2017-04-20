package org.reactivestreams;

public class ExpectPublisherException extends RuntimeException {

    public ExpectPublisherException(String message, String caller) {
        super(message + "\n\n from assertion at " + caller + "\n\n----------------------\n");
    }

}
