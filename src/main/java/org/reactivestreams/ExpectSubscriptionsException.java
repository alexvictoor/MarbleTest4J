package org.reactivestreams;

public class ExpectSubscriptionsException extends RuntimeException {

    public ExpectSubscriptionsException(String message, String caller) {
        super(message + "\n\n from assertion at " + caller + "\n\n----------------------\n");
    }

}
