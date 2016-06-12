package rx.marble;

import rx.Notification;
import rx.schedulers.TestScheduler;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


public class MarbleScheduler extends TestScheduler {

    private final long frameTimeFactor;

    public MarbleScheduler(long frameTimeFactor) {

        this.frameTimeFactor = frameTimeFactor;
    }

    public MarbleScheduler() {
        frameTimeFactor = 10;
    }

    public void flush() {
        advanceTimeTo(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
    }

    public <T> ColdObservable<T> createColdObservable(String marbles, Map<String, T> values) {
        List<Recorded<Notification<T>>> notifications = Parser.parseMarbles(marbles, values, null, frameTimeFactor);
        return ColdObservable.create(this, notifications);
    }


    public <T> HotObservable<T> createHotObservable(String marbles, Map<String, T> values) {
        List<Recorded<Notification<T>>> notifications = Parser.parseMarbles(marbles, values, null, frameTimeFactor);
        return HotObservable.create(this, notifications);
    }
}
