package reactor.junit;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.reactivestreams.ISetupSubscriptionsTest;
import org.reactivestreams.ISetupTest;
import org.reactivestreams.SubscriptionLog;
import reactor.ColdFlux;
import reactor.HotFlux;
import reactor.MarbleScheduler;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Created by Alexandre Victoor on 26/04/2017.
 */
public class MarbleRule implements TestRule {

    private static ThreadLocal<MarbleScheduler> schedulerHolder = new ThreadLocal<>();

    public final MarbleScheduler scheduler;

    public MarbleRule() {
        scheduler = new MarbleScheduler();
    }

    public MarbleRule(long frameTimeFactor) {
        scheduler = new MarbleScheduler(frameTimeFactor);
    }

    public static <T> HotFlux<T> hot(String marbles, Map<String, T> values) {
        return schedulerHolder.get().createHotFlux(marbles, values);
    }

    public static HotFlux<String> hot(String marbles) {
        return schedulerHolder.get().createHotFlux(marbles);
    }

    public static <T> ColdFlux<T> cold(String marbles, Map<String, T> values) {
        return schedulerHolder.get().createColdFlux(marbles, values);
    }

    public static ColdFlux<String> cold(String marbles) {
        return schedulerHolder.get().createColdFlux(marbles);
    }

    public static ISetupTest expectFlux(Flux<?> actual) {
        return schedulerHolder.get().expectFlux(actual);
    }

    public static ISetupTest expectFlux(Flux<?> actual, String unsubscriptionMarbles) {
        return schedulerHolder.get().expectFlux(actual, unsubscriptionMarbles);
    }

    public static ISetupTest expectMono(Mono<?> actual) {
        return schedulerHolder.get().expectFlux(actual.flux());
    }

    public static ISetupTest expectMono(Mono<?> actual, String unsubscriptionMarbles) {
        return schedulerHolder.get().expectFlux(actual.flux(), unsubscriptionMarbles);
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