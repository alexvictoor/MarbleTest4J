package io.reactivex.marble;


import io.reactivex.Notification;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.TestScheduler;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


public class MarbleScheduler extends Scheduler {

    private final List<ITestOnFlush> flushTests = new ArrayList<>();
    private final long frameTimeFactor;

    private final TestScheduler testScheduler = new TestScheduler();

    public MarbleScheduler(long frameTimeFactor) {

        this.frameTimeFactor = frameTimeFactor;
    }

    public MarbleScheduler() {
        frameTimeFactor = 10;
    }

    @Override
    public Worker createWorker() {
        // TODO
        return null;
    }

    public <T> ColdObservable<T> createColdObservable(String marbles, Map<String, T> values) {
        List<Recorded<T>> notifications = Parser.parseMarbles(marbles, values, null, frameTimeFactor);
        return ColdObservable.create(this, notifications);
    }

    public <T> ColdObservable<T> createColdObservable(String marbles) {
        return createColdObservable(marbles, null);
    }

    public <T> HotObservable<T> createHotObservable(String marbles, Map<String, T> values) {
        List<Recorded<T>> notifications = Parser.parseMarbles(marbles, values, null, frameTimeFactor);
        return HotObservable.create(this, notifications);
    }

    public <T> HotObservable<T> createHotObservable(String marbles) {
        return createHotObservable(marbles, null);
    }


    public long createTime(String marbles) {
        int endIndex = marbles.indexOf("|");
        if (endIndex == -1) {
            throw new RuntimeException("Marble diagram for time should have a completion marker '|'");
        }

        return endIndex * frameTimeFactor;
    }

    public void flush() {
        testScheduler.advanceTimeTo(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
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
        String caller = ExceptionHelper.findCallerInStackTrace(getClass());
        FlushableTest flushTest = new FlushableTest(caller);
        final List<Recorded<?>> actual = new ArrayList<>();
        flushTest.actual = actual;
        long unsubscriptionFrame = Long.MAX_VALUE;

        if (unsubscriptionMarbles != null) {
           unsubscriptionFrame
                    = Parser.parseMarblesAsSubscriptions(unsubscriptionMarbles, frameTimeFactor).unsubscribe;
        }
        final Disposable subscription
                = observable.subscribe(
                new Consumer<T>() {
                    @Override
                    public void accept(T x) {
                        Object value = x;
                        // Support Observable-of-Observables
                        if (value instanceof Observable) {
                            value = materializeInnerObservable((Observable)value, testScheduler.now(TimeUnit.MILLISECONDS));
                        }
                        actual.add(new Recorded<>(testScheduler.now(TimeUnit.MILLISECONDS), (Notification<?>) Notification.createOnNext(value)));
                    }
                },
                new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) {
                        actual.add(new Recorded<>(testScheduler.now(TimeUnit.MILLISECONDS), Notification.createOnError(throwable)));
                    }
                }, new Action() {
                    @Override
                    public void run() {
                        actual.add(new Recorded<>(testScheduler.now(TimeUnit.MILLISECONDS), Notification.createOnComplete()));
                    }
                });

        if (unsubscriptionFrame != Long.MAX_VALUE) {
            createWorker().schedule(new Runnable() {
                @Override
                public void run() {
                    subscription.dispose(); // todo check this is the same as unsub
                }
            }, unsubscriptionFrame, TimeUnit.MILLISECONDS);
        }

        flushTests.add(flushTest);

        return new SetupTest(flushTest, frameTimeFactor);
    }

    private List<Recorded<Object>> materializeInnerObservable(final Observable observable, final long outerFrame) {
        final List<Recorded<Object>> messages = new ArrayList<>();
        observable.subscribe(
                new Consumer() {
                    @Override
                    public void accept(Object x) {
                        messages.add(new Recorded<>(testScheduler.now(TimeUnit.MILLISECONDS) - outerFrame, Notification.createOnNext(x)));
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) {
                        messages.add(new Recorded<>(testScheduler.now(TimeUnit.MILLISECONDS) - outerFrame, Notification.createOnError(throwable)));
                    }
                }, new Action() {
                    @Override
                    public void run() {
                        messages.add(new Recorded<>(testScheduler.now(TimeUnit.MILLISECONDS) - outerFrame, Notification.createOnComplete()));
                    }
                });

        return messages;
    }

    public ISetupSubscriptionsTest expectSubscriptions(List<SubscriptionLog> subscriptions) {
        String caller = ExceptionHelper.findCallerInStackTrace(getClass());
        FlushableSubscriptionTest flushTest = new FlushableSubscriptionTest(caller);
        flushTest.actual = subscriptions;
        flushTests.add(flushTest);
        return new SetupSubscriptionsTest(flushTest, frameTimeFactor);
    }

    class SetupTest extends SetupTestSupport {
        private final FlushableTest flushTest;
        private final long frameTimeFactor;

        public SetupTest(FlushableTest flushTest, long frameTimeFactor) {
            this.flushTest = flushTest;
            this.frameTimeFactor = frameTimeFactor;
        }

        public void toBe(String marble, Map<String, ?> values, Exception errorValue) {
            flushTest.ready = true;
            if (values == null) {
                flushTest.expected = Parser.parseMarbles(marble, null, errorValue, frameTimeFactor, true);
            } else {
                flushTest.expected = Parser.parseMarbles(marble, new HashMap<>(values), errorValue, frameTimeFactor, true);
            }
        }
    }

    interface ITestOnFlush {
        void run();
        boolean isReady();
    }

    class FlushableTest implements ITestOnFlush {
        private final String caller;
        private boolean ready;
        public List<Recorded<?>> actual;
        public List expected;

        public FlushableTest(String caller) {
            this.caller = caller;
        }

        public void run() {

            RecordedStreamComparator.StreamComparison result
                    = new RecordedStreamComparator().compare(actual, expected);

            if (!result.streamEquals) {
                throw new ExpectObservableException(result.toString(), caller);
            }
        }

        @Override
        public boolean isReady() {
            return ready;
        }

    }

    class SetupSubscriptionsTest implements ISetupSubscriptionsTest {
        private final FlushableSubscriptionTest flushTest;
        private final long frameTimeFactor;

        public SetupSubscriptionsTest(FlushableSubscriptionTest flushTest, long frameTimeFactor) {
            this.flushTest = flushTest;
            this.frameTimeFactor = frameTimeFactor;
        }

        public void toBe(String... marbles) {
            flushTest.ready = true;
            flushTest.expected = new ArrayList<>();
            for (String marble : marbles) {
                SubscriptionLog subscriptionLog = Parser.parseMarblesAsSubscriptions(marble, frameTimeFactor);
                flushTest.expected.add(subscriptionLog);
            }
        }
    }

    class FlushableSubscriptionTest implements ITestOnFlush {
        private final String caller;
        private  boolean ready;
        public List<SubscriptionLog> actual;
        public List<SubscriptionLog> expected;

        public FlushableSubscriptionTest(String caller) {
            this.caller = caller;
        }

        public void run() {
            if (actual.size() != expected.size()) {
                throw new ExpectSubscriptionsException(
                        expected.size() + " subscription(s) expected, only " + actual.size() + " observed",
                        caller
                );
            }
            for (int i = 0; i < actual.size(); i++) {
                if ((actual.get(i) != null && !actual.get(i).equals(expected.get(i)))
                    || (actual.get(i) == null && expected.get(i) != null)) {
                    throw new ExpectSubscriptionsException(
                            "Expected subscription was " + expected.get(i) + ", instead received " + actual.get(i),
                            caller
                    );
                }
            }
        }

        @Override
        public boolean isReady() {
            return ready;
        }
    }
}
