package io.reactivex.marble;


import io.reactivex.Observable;
import io.reactivex.functions.Function;
import io.reactivex.observers.TestObserver;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static io.reactivex.marble.MapHelper.of;

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
        TestObserver<String> observer = new TestObserver<>();
        source.subscribe(observer);
        scheduler.advanceTimeBy(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        observer.assertValues("A", "B");
    }

    @Test
    public void should_create_a_cold_observable_taking_in_account_frame_time_factor() {
        scheduler = new MarbleScheduler(100);
        ColdObservable<String> source = scheduler.createColdObservable("--a---b--|", of("a", "A", "b", "B"));
        TestObserver<String> observer = new TestObserver<>();
        source.subscribe(observer);
        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);

        observer.assertNoValues();
    }

    @Test
    public void should_create_a_hot_observable() {
        HotObservable<String> source = scheduler.createHotObservable("--a---b--|", of("a", "A", "b", "B"));
        TestObserver<String> observer = new TestObserver<>();
        source.subscribe(observer);
        scheduler.advanceTimeBy(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        observer.assertValues("A", "B");
    }

    @Test
    public void should_create_a_hot_observable_taking_in_account_frame_time_factor() {
        scheduler = new MarbleScheduler(100);
        HotObservable<String> source = scheduler.createHotObservable("--a---b--|", of("a", "A", "b", "B"));
        TestObserver<String> observer = new TestObserver<>();
        source.subscribe(observer);
        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);

        observer.assertNoValues();
    }

    @Test
    public void should_create_a_hot_observable_sending_events_occurring_after_subscribe() {
        final HotObservable<String> source = scheduler.createHotObservable("--a---b--|", of("a", "A", "b", "B"));
        final TestObserver<String> observer = new TestObserver<>();
        scheduler.createWorker().schedule(new Runnable() {
            @Override
            public void run() {
                source.subscribe(observer);
            }
        }, 50, TimeUnit.MILLISECONDS);
        scheduler.advanceTimeBy(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        observer.assertValue("B");
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
        Observable<String> upperEvents = sourceEvents.map(new Function<String, String>() {
            @Override
            public String apply(String s) {
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
    @Ignore
    public void should_assert_subscriptions_of_a_cold_observable() {
        ColdObservable<String> source = scheduler.createColdObservable("---a---b-|");
        String subs =                                                  "^--------!";
        scheduler.expectSubscriptions(source.getSubscriptions()).toBe(subs);
        source.subscribe();
    }

    @Test
    @Ignore
    public void should_be_awesome() {
        Map<String, ?> values = of("a", 1, "b", 2);
        ColdObservable<?> myObservable
                = scheduler.createColdObservable(     "---a---b--|", values);
        String subs =                                 "^---------!";
        scheduler.expectObservable(myObservable).toBe("---a---b--|", values);
        scheduler.expectSubscriptions(myObservable.getSubscriptions()).toBe(subs);
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

    @Test
    public void should_indicate_failed_assertion_with_unexpected_observable() {
        MarbleScheduler scheduler = new MarbleScheduler();
        scheduler.expectObservable(Observable.just("hello")).toBe("--h|");
        try {
            scheduler.flush();
        } catch(ExpectObservableException ex) {
            assertThat(ex.getMessage()).contains("from assertion at " + getClass().getCanonicalName());
        }
    }

    @Test
    public void should_indicate_events_values_when_assertions_fails() {
        MarbleScheduler scheduler = new MarbleScheduler();
        scheduler.expectObservable(Observable.just("hello")).toBe("--h#", of("h", "hola"), new Exception());
        try {
            scheduler.flush();
        } catch(ExpectObservableException ex) {
            assertThat(ex.getMessage()).contains("hello");
            assertThat(ex.getMessage()).contains("On Error");
        }
    }

    @Test
    public void should_indicate_failed_assertion_when_no_expected_subscription() {
        MarbleScheduler scheduler = new MarbleScheduler();
        ColdObservable<?> myObservable
                = scheduler.createColdObservable(     "---a---b--|");
        String subs =                                 "^---------!";
        scheduler.expectSubscriptions(myObservable.getSubscriptions()).toBe(subs);
        try {
            scheduler.flush();
        } catch(ExpectSubscriptionsException ex) {
            assertThat(ex.getMessage()).contains("from assertion at " + getClass().getCanonicalName());
        }
    }

    @Test
    public void should_indicate_failed_assertion_with_unexpected_subscription() {
        MarbleScheduler scheduler = new MarbleScheduler();
        ColdObservable<?> myObservable
                = scheduler.createColdObservable(     "---a---b--|");
        String subs =                                 "^-----------!";
        scheduler.expectObservable(myObservable).toBe("---a---b--|");
        scheduler.expectSubscriptions(myObservable.getSubscriptions()).toBe(subs);
        try {
            scheduler.flush();
        } catch(ExpectSubscriptionsException ex) {
            assertThat(ex.getMessage()).contains("from assertion at " + getClass().getCanonicalName());
        }
    }

}