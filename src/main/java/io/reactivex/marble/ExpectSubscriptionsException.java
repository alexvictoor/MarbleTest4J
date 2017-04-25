package io.reactivex.marble;

public class ExpectSubscriptionsException extends RuntimeException {

    public ExpectSubscriptionsException(String message, String caller) {
        super(message + "\n\n from assertion at " + caller + "\n\n----------------------\n");
    }


    public ExpectSubscriptionsException(String message) {
        super(message);
    }
}
