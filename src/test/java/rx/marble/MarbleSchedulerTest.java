package rx.marble;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import rx.Observable;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.observers.TestSubscriber;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static rx.marble.MapHelper.of;

public class MarbleSchedulerTest {

    private MarbleScheduler scheduler;

    @Before
    public void setupScheduler() {
        scheduler = new MarbleScheduler();
    }

    @After
    public void flushScheduler() {
        if (scheduler != null) {
            scheduler.flush();
        }
    }

    @Test
    public void should_create_a_cold_observable() {
        ColdObservable<String> source = scheduler.createColdObservable("--a---b--|", of("a", "A", "b", "B"));
        TestSubscriber<String> subscriber = new TestSubscriber<>();
        source.subscribe(subscriber);
        scheduler.advanceTimeBy(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        assertThat(subscriber.getOnNextEvents())
                .containsExactly("A", "B");
    }

    @Test
    public void should_create_a_cold_observable_taking_in_account_frame_time_factor() {
        scheduler = new MarbleScheduler(100);
        ColdObservable<String> source = scheduler.createColdObservable("--a---b--|", of("a", "A", "b", "B"));
        TestSubscriber<String> subscriber = new TestSubscriber<>();
        source.subscribe(subscriber);
        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);

        assertThat(subscriber.getOnNextEvents()).isEmpty();
    }

    @Test
    public void should_create_a_hot_observable() {
        HotObservable<String> source = scheduler.createHotObservable("--a---b--|", of("a", "A", "b", "B"));
        TestSubscriber<String> subscriber = new TestSubscriber<>();
        source.subscribe(subscriber);
        scheduler.advanceTimeBy(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        assertThat(subscriber.getOnNextEvents())
                .containsExactly("A", "B");
    }

    @Test
    public void should_create_a_hot_observable_taking_in_account_frame_time_factor() {
        scheduler = new MarbleScheduler(100);
        HotObservable<String> source = scheduler.createHotObservable("--a---b--|", of("a", "A", "b", "B"));
        TestSubscriber<String> subscriber = new TestSubscriber<>();
        source.subscribe(subscriber);
        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);

        assertThat(subscriber.getOnNextEvents()).isEmpty();
    }

    @Test
    public void should_create_a_hot_observable_sending_events_occurring_after_subscribe() {
        final HotObservable<String> source = scheduler.createHotObservable("--a---b--|", of("a", "A", "b", "B"));
        final TestSubscriber<String> subscriber = new TestSubscriber<>();
        scheduler.createWorker().schedule(new Action0() {
            @Override
            public void call() {
                source.subscribe(subscriber);
            }
        }, 50, TimeUnit.MILLISECONDS);
        scheduler.advanceTimeBy(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        assertThat(subscriber.getOnNextEvents())
                .containsExactly("B");
    }

    @Test
    public void should_parse_a_simple_time_marble_string_to_a_number() {
        long time = scheduler.createTime("-----|");
        assertThat(time).isEqualTo(50l);
    }

    @Test(expected = RuntimeException.class)
    public void should_throw_if_not_given_good_marble_input() {
        scheduler.createTime("-a-b-c-#");
    }

    @Test
    public void should_expect_empty_observable() {
        scheduler.expectObservable(Observable.empty()).toBe("|", Collections.<String, Object>emptyMap());
    }

    @Test
    public void should_expect_never_observable() {
        scheduler.expectObservable(Observable.never()).toBe("-", Collections.<String, Object>emptyMap());
        scheduler.expectObservable(Observable.never()).toBe("---", Collections.<String, Object>emptyMap());
    }

    @Test
    public void should_expect_one_value_observable() {
        scheduler.expectObservable(Observable.just("hello")).toBe("(h|)", of("h", (Object)"hello"));
    }

    @Test(expected = RuntimeException.class)
    public void should_fail_when_event_values_differ() {
        MarbleScheduler scheduler = new MarbleScheduler();
        scheduler.expectObservable(Observable.just("hello")).toBe("(h|)", of("h", (Object)"bye"));
        scheduler.flush();
    }

    @Test(expected = RuntimeException.class)
    public void should_fail_when_event_timing_differs() {
        MarbleScheduler scheduler = new MarbleScheduler();
        scheduler.expectObservable(Observable.just("hello")).toBe("--h|", of("h", (Object)"hello"));
        scheduler.flush();
    }

    @Test
    public void should_compare_streams_with_multiple_events() {
        Observable<String> sourceEvents = scheduler.createColdObservable("a-b-c-|");
        Observable<String> upperEvents = sourceEvents.map(new Func1<String, String>() {
            @Override
            public String call(String s) {
                return s.toUpperCase();
            }
        });
        scheduler.expectObservable(upperEvents).toBe("A-B-C-|");
    }

    @Test
    public void should_use_unsubscription_diagram() {
        Observable<String> source = scheduler.createHotObservable("---^-a-b-|");
        String unsubscribe =                                         "---!";
        String expected =                                            "--a";
        scheduler.expectObservable(source, unsubscribe).toBe(expected);
    }

    @Test
    public void should_assert_subscriptions_of_a_cold_observable() {
        ColdObservable<String> source = scheduler.createColdObservable("---a---b-|");
        String subs =                                                  "^--------!";
        scheduler.expectSubscriptions(source.subscriptions).toBe(subs);
        source.subscribe();
    }

    @Test
    public void should_be_awesome() {
        Map<String, ?> values = of("a", 1, "b", 2);
        ColdObservable<?> myObservable
                = scheduler.createColdObservable(     "---a---b--|", values);
        String subs =                                 "^---------!";
        scheduler.expectObservable(myObservable).toBe("---a---b--|", values);
        scheduler.expectSubscriptions(myObservable.subscriptions).toBe(subs);
    }

    @Test
    public void should_support_testing_metastreams()
    {
        ColdObservable<String> x = scheduler.createColdObservable("-a-b|");
        ColdObservable<String> y = scheduler.createColdObservable("-c-d|");
        Observable<ColdObservable<String>> myObservable
                = scheduler.createHotObservable("---x---y----|", of("x", x, "y", y));
        String expected = "---x---y----|";
        Object expectedx = scheduler.createColdObservable("-a-b|");
        Object expectedy = scheduler.createColdObservable("-c-d|");
        scheduler.expectObservable(myObservable).toBe(expected, of("x", expectedx, "y", expectedy));
    }

    @Test
    public void should_demo_metastreams_with_windows() {
        String input   =                                "a---b---c---d-|";
        Observable<String> myObservable = scheduler.createColdObservable(input);

        Observable<?> result = myObservable.window(2, 1);

        Object aWindow = scheduler.createColdObservable("a---(b|)");
        Object bWindow = scheduler.createColdObservable(    "b---(c|)");
        Object cWindow = scheduler.createColdObservable(        "c---(d|)");
        Object dWindow = scheduler.createColdObservable(            "d-|");

        String expected = "a---b---c---d-|";
        scheduler
                .expectObservable(result)
                .toBe(expected, of("a", aWindow, "b", bWindow, "c", cWindow, "d", dWindow));
    }

}