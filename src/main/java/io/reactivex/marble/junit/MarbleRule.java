package io.reactivex.marble.junit;


import io.reactivex.*;
import io.reactivex.marble.*;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.reactivestreams.ISetupSubscriptionsTest;
import org.reactivestreams.ISetupTest;
import org.reactivestreams.SubscriptionLog;

import java.util.List;
import java.util.Map;

public class MarbleRule implements TestRule {

    private static ThreadLocal<MarbleScheduler> schedulerHolder = new ThreadLocal<>();

    public final MarbleScheduler scheduler;

    public MarbleRule() {
        scheduler = new MarbleScheduler();
    }

    public MarbleRule(long frameTimeFactor) {
        scheduler = new MarbleScheduler(frameTimeFactor);
    }

    public static <T> HotObservable<T> hot(String marbles, Map<String, T> values) {
        return schedulerHolder.get().createHotObservable(marbles, values);
    }

    public static HotObservable<String> hot(String marbles) {
        return schedulerHolder.get().createHotObservable(marbles);
    }

    public static <T> ColdObservable<T> cold(String marbles, Map<String, T> values) {
        return schedulerHolder.get().createColdObservable(marbles, values);
    }

    public static ColdObservable<String> cold(String marbles) {
        return schedulerHolder.get().createColdObservable(marbles);
    }

    public static ISetupTest expectObservable(Observable<?> actual) {
        return schedulerHolder.get().expectObservable(actual);
    }

    public static ISetupTest expectObservable(Observable<?> actual, String unsubscriptionMarbles) {
        return schedulerHolder.get().expectObservable(actual, unsubscriptionMarbles);
    }

    public static ISetupTest expectFlowable(Flowable<?> actual) {
        return schedulerHolder.get().expectFlowable(actual);
    }

    public static ISetupTest expectFlowable(Flowable<?> actual, String unsubscriptionMarbles) {
        return schedulerHolder.get().expectFlowable(actual, unsubscriptionMarbles);
    }

    public static ISetupTest expectSingle(Single<?> actual) {
        return schedulerHolder.get().expectFlowable(actual.toFlowable());
    }

    public static ISetupTest expectSingle(Single<?> actual, String unsubscriptionMarbles) {
        return schedulerHolder.get().expectFlowable(actual.toFlowable(), unsubscriptionMarbles);
    }

    public static ISetupTest expectMaybe(Maybe<?> actual) {
        return schedulerHolder.get().expectFlowable(actual.toFlowable());
    }

    public static ISetupTest expectCompletable(Completable actual, String unsubscriptionMarbles) {
        return schedulerHolder.get().expectFlowable(actual.toFlowable(), unsubscriptionMarbles);
    }

    public static ISetupTest expectCompletable(Completable actual) {
        return schedulerHolder.get().expectFlowable(actual.toFlowable());
    }

    public static ISetupTest expectMaybe(Maybe<?> actual, String unsubscriptionMarbles) {
        return schedulerHolder.get().expectObservable(actual.toObservable(), unsubscriptionMarbles);
    }

    public static ISetupSubscriptionsTest expectSubscriptions(List<SubscriptionLog> subscriptions) {
        return schedulerHolder.get().expectSubscriptions(subscriptions);
    }

    @Override
    public Statement apply(final Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                schedulerHolder.set(scheduler);
                try {
                    base.evaluate();
                    scheduler.flush();
                } finally {
                    schedulerHolder.remove();
                }

            }
        };
    }
}
