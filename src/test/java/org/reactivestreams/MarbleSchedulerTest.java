package org.reactivestreams;

import io.reactivex.subscribers.TestSubscriber;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import reactor.ColdFlux;
import reactor.HotFlux;
import reactor.MarbleScheduler;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

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
        ColdFlux<String> source = scheduler.createColdFlux("--a---b--|", of("a", "A", "b", "B"));
        TestSubscriber<String> observer = new TestSubscriber<>();
        source.subscribe(observer);
        scheduler.advanceTimeBy(Duration.ofNanos(Long.MAX_VALUE));
        observer.assertValues("A", "B");
    }

    @Test
    public void should_create_a_cold_observable_taking_in_account_frame_time_factor() {
        scheduler = new MarbleScheduler(100);
        ColdFlux<String> source = scheduler.createColdFlux("--a---b--|", of("a", "A", "b", "B"));
        TestSubscriber<String> observer = new TestSubscriber<>();
        source.subscribe(observer);
        scheduler.advanceTimeBy(Duration.ofMillis(100));

        observer.assertNoValues();
    }

    @Test
    public void should_create_a_hot_observable() {
        HotFlux<String> source = scheduler.createHotFlux("--a---b--|", of("a", "A", "b", "B"));
        TestSubscriber<String> observer = new TestSubscriber<>();
        source.subscribe(observer);
        scheduler.advanceTimeBy(Duration.ofNanos(Long.MAX_VALUE));
        observer.assertValues("A", "B");
    }

    @Test
    public void should_create_a_hot_observable_taking_in_account_frame_time_factor() {
        scheduler = new MarbleScheduler(100);
        HotFlux<String> source = scheduler.createHotFlux("--a---b--|", of("a", "A", "b", "B"));
        TestSubscriber<String> observer = new TestSubscriber<>();
        source.subscribe(observer);
        scheduler.advanceTimeBy(Duration.ofMillis(100));

        observer.assertNoValues();
    }

    @Test
    public void should_create_a_hot_observable_sending_events_occurring_after_subscribe() {
        final HotFlux<String> source = scheduler.createHotFlux("--a---b--|", of("a", "A", "b", "B"));
        final TestSubscriber<String> observer = new TestSubscriber<>();
        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                source.subscribe(observer);
            }
        }, 50, TimeUnit.MILLISECONDS);
        scheduler.advanceTimeBy(Duration.ofNanos(Long.MAX_VALUE));
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
        scheduler.expectFlux(Flux.empty()).toBe("|", Collections.<String, Object>emptyMap());
    }

    @Test
    public void should_expect_never_observable() {
        scheduler.expectFlux(Flux.never()).toBe("-", Collections.<String, Object>emptyMap());
        scheduler.expectFlux(Flux.never()).toBe("---", Collections.<String, Object>emptyMap());
    }

    @Test
    public void should_expect_one_value_observable() {
        scheduler.expectFlux(Flux.just("hello")).toBe("(h|)", of("h", (Object)"hello"));
    }

    @Test(expected = RuntimeException.class)
    public void should_fail_when_event_values_differ() {
        MarbleScheduler scheduler = new MarbleScheduler();
        scheduler.expectFlux(Flux.just("hello")).toBe("(h|)", of("h", (Object)"bye"));
        scheduler.flush();
    }

    @Test(expected = RuntimeException.class)
    public void should_fail_when_event_timing_differs() {
        MarbleScheduler scheduler = new MarbleScheduler();
        scheduler.expectFlux(Flux.just("hello")).toBe("--h|", of("h", (Object)"hello"));
        scheduler.flush();
    }

    @Test
    public void should_compare_streams_with_multiple_events() {
        Flux<String> sourceEvents = scheduler.createColdFlux("a-b-c-|");
        Flux<String> upperEvents = sourceEvents.map(new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s.toUpperCase();
            }
        });
        scheduler.expectFlux(upperEvents).toBe("A-B-C-|");
    }

    @Test
    public void should_use_unsubscription_diagram() {
        Flux<String> source = scheduler.createHotFlux("---^-a-b-|");
        String unsubscribe =                                         "---!";
        String expected =                                            "--a";
        scheduler.expectFlux(source, unsubscribe).toBe(expected);
    }

    @Test
    public void should_assert_subscriptions_of_a_cold_observable() {
        ColdFlux<String> source = scheduler.createColdFlux("---a---b-|");
        String subs =                                                  "^--------!";
        scheduler.expectSubscriptions(source.getSubscriptions()).toBe(subs);
        source.subscribe();
    }

    @Test
    public void should_assert_subscriptions_of_a_hot_observable() {
        HotFlux<String> source = scheduler.createHotFlux("---a---b-|");
        String subs =                                                  "^--------!";
        scheduler.expectSubscriptions(source.getSubscriptions()).toBe(subs);
        source.subscribe();
    }

    @Test
    public void should_be_awesome() {
        Map<String, ?> values = of("a", 1, "b", 2);
        ColdFlux<?> myFlux
                = scheduler.createColdFlux(     "---a---b--|", values);
        String subs =                                 "^---------!";
        scheduler.expectFlux(myFlux).toBe("---a---b--|", values);
        scheduler.expectSubscriptions(myFlux.getSubscriptions()).toBe(subs);
    }

    @Test
    public void should_support_testing_metastreams()
    {
        ColdFlux<String> x = scheduler.createColdFlux("-a-b|");
        ColdFlux<String> y = scheduler.createColdFlux("-c-d|");
        Flux<ColdFlux<String>> myFlux
                = scheduler.createHotFlux("---x---y----|", of("x", x, "y", y));
        String expected = "---x---y----|";
        Object expectedx = scheduler.createColdFlux("-a-b|");
        Object expectedy = scheduler.createColdFlux("-c-d|");
        scheduler.expectFlux(myFlux).toBe(expected, of("x", expectedx, "y", expectedy));
    }

    @Test
    public void should_demo_metastreams_with_windows() {
        String input   =                                "a---b---c---d-|";
        Flux<String> myFlux = scheduler.createColdFlux(input);

        Flux<?> result = myFlux.window(2, 1);

        Object aWindow = scheduler.createColdFlux("a---(b|)");
        Object bWindow = scheduler.createColdFlux(    "b---(c|)");
        Object cWindow = scheduler.createColdFlux(        "c---(d|)");
        Object dWindow = scheduler.createColdFlux(            "d-|");

        String expected = "a---b---c---d-|";
        scheduler
                .expectFlux(result)
                .toBe(expected, of("a", aWindow, "b", bWindow, "c", cWindow, "d", dWindow));
    }

    @Test
    public void should_indicate_failed_assertion_with_unexpected_observable() {
        MarbleScheduler scheduler = new MarbleScheduler();
        scheduler.expectFlux(Flux.just("hello")).toBe("--h|");
        try {
            scheduler.flush();
        } catch(ExpectPublisherException ex) {
            assertThat(ex.getMessage()).contains("from assertion at " + getClass().getCanonicalName());
        }
    }

    @Test
    public void should_indicate_events_values_when_assertions_fails() {
        MarbleScheduler scheduler = new MarbleScheduler();
        scheduler.expectFlux(Flux.just("hello")).toBe("--h#", of("h", "hola"), new Exception());
        try {
            scheduler.flush();
        } catch(ExpectPublisherException ex) {
            assertThat(ex.getMessage()).contains("hello");
            assertThat(ex.getMessage()).contains("On Error");
        }
    }

    @Test
    public void should_indicate_failed_assertion_when_no_expected_subscription() {
        MarbleScheduler scheduler = new MarbleScheduler();
        ColdFlux<?> myFlux
                = scheduler.createColdFlux(     "---a---b--|");
        String subs =                                 "^---------!";
        scheduler.expectSubscriptions(myFlux.getSubscriptions()).toBe(subs);
        try {
            scheduler.flush();
        } catch(ExpectSubscriptionsException ex) {
            assertThat(ex.getMessage()).contains("from assertion at " + getClass().getCanonicalName());
        }
    }

    @Test
    public void should_indicate_failed_assertion_with_unexpected_subscription() {
        MarbleScheduler scheduler = new MarbleScheduler();
        ColdFlux<?> myFlux
                = scheduler.createColdFlux(     "---a---b--|");
        String subs =                                 "^-----------!";
        scheduler.expectFlux(myFlux).toBe("---a---b--|");
        scheduler.expectSubscriptions(myFlux.getSubscriptions()).toBe(subs);
        try {
            scheduler.flush();
        } catch(ExpectSubscriptionsException ex) {
            assertThat(ex.getMessage()).contains("from assertion at " + getClass().getCanonicalName());
        }
    }

}