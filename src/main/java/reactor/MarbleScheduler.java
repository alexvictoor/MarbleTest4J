package reactor;

import org.reactivestreams.*;
import reactor.core.publisher.Flux;
import reactor.test.scheduler.VirtualTimeScheduler;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by Alexandre Victoor on 20/04/2017.
 */
public class MarbleScheduler extends VirtualTimeScheduler {
    private final MarbleSchedulerState state;
    private final long frameTimeFactor;

    public MarbleScheduler() {
        this(10);
    }

    public MarbleScheduler(long frameTimeFactor) {
        this.frameTimeFactor = frameTimeFactor;
        state = new MarbleSchedulerState(frameTimeFactor, new MarbleSchedulerState.ISchedule() {
            @Override
            public long now() {
                return MarbleScheduler.this.now(TimeUnit.MILLISECONDS);
            }

            @Override
            public void schedule(Runnable runnable, long time) {
                MarbleScheduler.this.schedule(runnable, time, TimeUnit.MILLISECONDS);
            }
        }, getClass());
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
        state.flush();
    }

    public <T> ISetupTest expectFlux(Flux<T> flux) {
        return expectFlux(flux, null);
    }

    public <T> ISetupTest expectFlux(Flux<T> flux, String unsubscriptionMarbles) {
        return state.expectPublisher(flux, unsubscriptionMarbles);
    }

    public ISetupSubscriptionsTest expectSubscriptions(List<SubscriptionLog> subscriptions) {
        return state.expectSubscriptions(subscriptions);
    }
}
