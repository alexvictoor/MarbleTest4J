package reactor;

import org.reactivestreams.*;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.test.scheduler.VirtualTimeScheduler;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Created by Alexandre Victoor on 20/04/2017.
 */
public class MarbleScheduler extends VirtualTimeScheduler {
    private final List<ITestOnFlush> flushTests = new ArrayList<>();
    private final long frameTimeFactor;

    public MarbleScheduler(long frameTimeFactor) {

        this.frameTimeFactor = frameTimeFactor;
    }

    public MarbleScheduler() {
        frameTimeFactor = 10;
    }

    public <T> ColdFlux<T> createColdFlux(String marbles, Map<String, T> values) {
        List<Recorded<T>> notifications = Parser.parseMarbles(marbles, values, null, frameTimeFactor);
        return ColdFlux.create(this, notifications);
    }

    public <T> ColdFlux<T> createColdFlux(String marbles) {
        return createColdFlux(marbles, null);
    }

    public <T> HotFlux<T> createHotFlux(String marbles, Map<String, T> values) {
        List<Recorded<T>> notifications = Parser.parseMarbles(marbles, values, null, frameTimeFactor);
        return HotFlux.create(this, notifications);
    }

    public <T> HotFlux<T> createHotFlux(String marbles) {
        return createHotFlux(marbles, null);
    }


    public long createTime(String marbles) {
        int endIndex = marbles.indexOf("|");
        if (endIndex == -1) {
            throw new RuntimeException("Marble diagram for time should have a completion marker '|'");
        }

        return endIndex * frameTimeFactor;
    }

    public void flush() {
        advanceTimeTo(Instant.ofEpochMilli(Long.MAX_VALUE));
        for (ITestOnFlush test: flushTests) {
            if (test.isReady()) {
                test.run();
            }
        }
    }

    public <T> ISetupTest expectFlux(Flux<T> observable) {
        return expectFlux(observable, null);
    }

    public <T> ISetupTest expectFlux(Flux<T> observable, String unsubscriptionMarbles) {
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
                        // Support Flux-of-Fluxs
                        if (value instanceof Flux) {
                            value = materializeInnerFlux((Flux)value, now(TimeUnit.MILLISECONDS));
                        }
                        actual.add(new Recorded<>(now(TimeUnit.MILLISECONDS), (Notification<?>)Notification.createOnNext(value)));
                    }
                },
                new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) {
                        actual.add(new Recorded<>(now(TimeUnit.MILLISECONDS), Notification.createOnError(throwable)));
                    }
                }, new Runnable() {
                    @Override
                    public void run() {
                        actual.add(new Recorded<>(now(TimeUnit.MILLISECONDS), Notification.createOnComplete()));
                    }
                });

        if (unsubscriptionFrame != Long.MAX_VALUE) {
            schedule(new Runnable() {
                @Override
                public void run() {
                    subscription.dispose();
                }
            }, unsubscriptionFrame, TimeUnit.MILLISECONDS);
        }

        flushTests.add(flushTest);

        return new SetupTest(flushTest, frameTimeFactor);
    }

    private List<Recorded<Object>> materializeInnerFlux(final Flux observable, final long outerFrame) {
        final List<Recorded<Object>> messages = new ArrayList<>();
        observable.subscribe(
                new Consumer() {
                    @Override
                    public void accept(Object x) {
                        messages.add(new Recorded<>(now(TimeUnit.MILLISECONDS) - outerFrame, Notification.createOnNext(x)));
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) {
                        messages.add(new Recorded<>(now(TimeUnit.MILLISECONDS) - outerFrame, Notification.createOnError(throwable)));
                    }
                }, new Runnable() {
                    @Override
                    public void run() {
                        messages.add(new Recorded<>(now(TimeUnit.MILLISECONDS) - outerFrame, Notification.createOnComplete()));
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
                throw new ExpectPublisherException(result.toString(), caller);
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
