package rx.marble;

import com.sun.media.jfxmediaimpl.MediaDisposer;
import rx.Notification;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.TestScheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


public class MarbleScheduler extends TestScheduler {

    private final List<ITestOnFlush> flushTests = new ArrayList<>();
    private final long frameTimeFactor;

    public MarbleScheduler(long frameTimeFactor) {

        this.frameTimeFactor = frameTimeFactor;
    }

    public MarbleScheduler() {
        frameTimeFactor = 10;
    }

    public <T> ColdObservable<T> createColdObservable(String marbles, Map<String, T> values) {
        List<Recorded<Notification<T>>> notifications = Parser.parseMarbles(marbles, values, null, frameTimeFactor);
        return ColdObservable.create(this, notifications);
    }

    public <T> ColdObservable<T> createColdObservable(String marbles) {
        return createColdObservable(marbles, null);
    }

    public <T> HotObservable<T> createHotObservable(String marbles, Map<String, T> values) {
        List<Recorded<Notification<T>>> notifications = Parser.parseMarbles(marbles, values, null, frameTimeFactor);
        return HotObservable.create(this, notifications);
    }

    public <T> HotObservable<T> createHotObservable(String marbles) {
        return createHotObservable(marbles, null);
    }


    public long createTime(String marbles) {
        int endIndex = marbles.indexOf("|");
        if (endIndex == -1)
        {
            throw new RuntimeException("Marble diagram for time should have a completion marker '|'");
        }

        return endIndex * frameTimeFactor;
    }

    public void flush() {
        advanceTimeTo(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        for (ITestOnFlush test: flushTests) {
            if (test.isReady()) {
                test.run();
            }
        }
    }

    public <T> ISetupTest expectObservable(Observable<T> observable) {
        return expectObservable(observable, null);
    }

    public <T> ISetupTest expectObservable(Observable<T> observable, String unsubscriptionMarbles) {
        final List<Recorded<Notification<Object>>> actual = new ArrayList<>();
        FlushableTest flushTest = new FlushableTest();
        flushTest.actual = actual;
        long unsubscriptionFrame = Long.MAX_VALUE;

        if (unsubscriptionMarbles != null)
        {
            //unsubscriptionFrame
            //        = Parser.parseMarblesAsSubscriptions(unsubscriptionMarbles, _frameTimeFactor).Unsubscribe;
        }
        final Subscription subscription
                = observable.subscribe(
                new Action1<T>() {
                    @Override
                    public void call(T x) {
                        Object value = x;
                        // Support Observable-of-Observables
                        /*if (value is IObservable<object>)
                        {
                            value = MaterializeInnerObservable(value as IObservable<object>, Clock);
                        }*/
                        actual.add(new Recorded<>(now(), Notification.createOnNext(value)));
                    }
                },
                new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        actual.add(new Recorded<>(now(), Notification.createOnError(throwable)));
                    }
                }, new Action0() {
                    @Override
                    public void call() {
                        actual.add(new Recorded<>(now(), Notification.createOnCompleted()));
                    }
                });

        if (unsubscriptionFrame != Long.MAX_VALUE) {
            createWorker().schedule(new Action0() {
                @Override
                public void call() {
                    subscription.unsubscribe();
                }
            }, unsubscriptionFrame, TimeUnit.MILLISECONDS);
        }

        flushTests.add(flushTest);

        return new SetupTest(flushTest, frameTimeFactor);
    }

    class SetupTest extends SetupTestSupport
    {
        private final FlushableTest flushTest;
        private final long frameTimeFactor;

        public SetupTest(FlushableTest flushTest, long frameTimeFactor)
        {
            this.flushTest = flushTest;
            this.frameTimeFactor = frameTimeFactor;
        }

        public void toBe(String marble, Map<String, Object> values, Exception errorValue)
        {
            flushTest.ready = true;
            flushTest.expected = Parser.parseMarbles(marble, values, errorValue, frameTimeFactor, true);
        }
    }

    interface ITestOnFlush
    {
        void run();
        boolean isReady();
        void flagAsReady();
    }

    class FlushableTest implements ITestOnFlush {
        private  boolean ready;
        public List<Recorded<Notification<Object>>> actual;
        public List<Recorded<Notification<Object>>> expected;

        public void run() {
            if (actual.size() != expected.size()) {
                throw new RuntimeException(
                        expected.size() + " event(s) expected, "
                                + actual.size() + " observed");
            }

            for (int i = 0; i < actual.size(); i++) {

                Recorded<Notification<Object>> actualEvent = actual.get(i);
                Recorded<Notification<Object>> expectedEvent = expected.get(i);
                if (actualEvent.time != expectedEvent.time) {
                    throw new RuntimeException(
                            "Expected event \"" + expectedEvent.value + "\" at " + expectedEvent.time
                                    + ", instead received \"" + actualEvent.value + "\" at " + actualEvent.time);
                }

                if (actualEvent.value.getKind() != expectedEvent.value.getKind()) {
                    throw new RuntimeException(
                            "Expected event " + expectedEvent.value.getKind()
                                    + ", instead received at " + actualEvent.value.getKind());
                }

                if ((actualEvent.value.getValue() != null
                        && !actualEvent.value.getValue().equals(expectedEvent.value.getValue()))
                    || (actualEvent.value.getValue() == null && expectedEvent.value.getValue() != null)) {

                    throw new RuntimeException(
                            "Expected event was " + expectedEvent.value
                                    + ", instead received " + actualEvent.value);
                }
            }
        }

        @Override
        public boolean isReady() {
            return ready;
        }

        @Override
        public void flagAsReady() {
            ready = true;
        }

    }
}