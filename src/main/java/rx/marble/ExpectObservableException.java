package rx.marble;

public class ExpectObservableException extends RuntimeException {

    public ExpectObservableException(String message, String caller) {
        super(message + "\n\n from assertion at " + caller + "\n\n----------------------\n");
    }

}
