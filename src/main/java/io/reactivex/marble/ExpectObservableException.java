package io.reactivex.marble;

public class ExpectObservableException extends RuntimeException {

    public ExpectObservableException(String message, String caller) {
        super(message + "\n\n from assertion at " + caller + "\n\n----------------------\n");
    }

    public ExpectObservableException(String message) {
        super(message);
    }

}
