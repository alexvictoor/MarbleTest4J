package org.reactivestreams;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Alexandre Victoor on 23/04/2017.
 */
public class MarbleSchedulerState {

    private final List<ITestOnFlush> flushTests = new ArrayList<>();
    private final long frameTimeFactor;
    private final ISchedule scheduler;
    private final Class schedulerClass;

    public MarbleSchedulerState(long frameTimeFactor, ISchedule scheduler, Class schedulerClass) {

        this.frameTimeFactor = frameTimeFactor;
        this.scheduler = scheduler;
        this.schedulerClass = schedulerClass;
    }


    public void flush() {
        for (ITestOnFlush test: flushTests) {
            if (test.isReady()) {
                test.run();
            }
        }
    }

    public <T> ISetupTest expectPublisher(Publisher<T> publisher, String unsubscriptionMarbles) {
        String caller = ExceptionHelper.findCallerInStackTrace(schedulerClass, getClass());
        FlushableTest flushTest = new FlushableTest(caller);
        final List<Recorded<?>> actual = new ArrayList<>();
        flushTest.actual = actual;
        long unsubscriptionFrame = Long.MAX_VALUE;

        if (unsubscriptionMarbles != null) {
            unsubscriptionFrame
                    = Parser.parseMarblesAsSubscriptions(unsubscriptionMarbles, frameTimeFactor).unsubscribe;
        }
        final SubscriberForExpect<T> subscriber = new SubscriberForExpect<>(actual, scheduler);
        publisher.subscribe(subscriber);

        if (unsubscriptionFrame != Long.MAX_VALUE) {
            scheduler.schedule(new Runnable() {
                @Override
                public void run() {
                    subscriber.subscription.cancel();
                }
            }, unsubscriptionFrame);
        }

        flushTests.add(flushTest);

        return new SetupTest(flushTest, frameTimeFactor);
    }

    private List<Recorded<Object>> materializeInnerPublisher(final Publisher publisher, final ISchedule clock) {
        final List<Recorded<Object>> messages = new ArrayList<>();
        final long outerFrame = clock.now();
        publisher.subscribe(new Subscriber() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(Object x) {
                messages.add(new Recorded<>(clock.now() - outerFrame, Notification.createOnNext(x)));
            }

            @Override
            public void onError(Throwable throwable) {
                messages.add(new Recorded<>(clock.now() - outerFrame, Notification.createOnError(throwable)));
            }

            @Override
            public void onComplete() {
                messages.add(new Recorded<>(clock.now() - outerFrame, Notification.createOnComplete()));
            }
        });

        return messages;
    }

    private class SubscriberForExpect<T> implements Subscriber<T> {

        public Subscription subscription;
        private final List<Recorded<?>> actual;
        private final ISchedule clock;

        public SubscriberForExpect(List<Recorded<?>> actual, ISchedule clock) {
            this.actual = actual;
            this.clock = clock;
        }

        @Override
        public void onSubscribe(Subscription s) {
            subscription = s;
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(T x) {
            Object value = x;
            // Support Publisher-of-Publishers
            if (value instanceof Publisher) {
                value = materializeInnerPublisher((Publisher) value, clock);
            }
            actual.add(new Recorded<>(clock.now(), (Notification<?>)Notification.createOnNext(value)));
        }

        @Override
        public void onError(Throwable throwable) {
            actual.add(new Recorded<>(clock.now(), Notification.createOnError(throwable)));
        }

        @Override
        public void onComplete() {
            actual.add(new Recorded<>(clock.now(), Notification.createOnComplete()));
        }
    }


    public ISetupSubscriptionsTest expectSubscriptions(List<SubscriptionLog> subscriptions) {
        String caller = ExceptionHelper.findCallerInStackTrace(schedulerClass, getClass());
        FlushableSubscriptionTest flushTest = new FlushableSubscriptionTest(caller);
        flushTest.actual = subscriptions;
        flushTests.add(flushTest);
        return new SetupSubscriptionsTest(flushTest, frameTimeFactor);
    }

    public interface ISchedule {
        long now();
        void schedule(Runnable runnable, long time);
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
