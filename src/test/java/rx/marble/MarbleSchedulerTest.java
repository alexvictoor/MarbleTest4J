package rx.marble;


import com.sun.tools.javac.comp.Check;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import rx.functions.Action0;
import rx.observers.TestSubscriber;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static rx.marble.MapHelper.of;

public class MarbleSchedulerTest {

    private MarbleScheduler scheduler;

    @Before
    public void setupScheduler()
    {
        scheduler = new MarbleScheduler();
    }

    @After
    public void flushScheduler()
    {
        if (scheduler != null)
        {
            scheduler.flush();
        }
    }

    @Test
    public void should_create_a_cold_observable()
    {
        ColdObservable<String> source = scheduler.createColdObservable("--a---b--|", of("a", "A", "b", "B"));
        TestSubscriber<String> subscriber = new TestSubscriber<>();
        source.subscribe(subscriber);
        scheduler.advanceTimeBy(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        assertThat(subscriber.getOnNextEvents())
                .containsExactly("A", "B");
    }

    @Test
    public void should_create_a_cold_observable_taking_in_account_frame_time_factor()
    {
        scheduler = new MarbleScheduler(100);
        ColdObservable<String> source = scheduler.createColdObservable("--a---b--|", of("a", "A", "b", "B"));
        TestSubscriber<String> subscriber = new TestSubscriber<>();
        source.subscribe(subscriber);
        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);

        assertThat(subscriber.getOnNextEvents()).isEmpty();
    }

    @Test
    public void should_create_a_hot_observable()
    {
        HotObservable<String> source = scheduler.createHotObservable("--a---b--|", of("a", "A", "b", "B"));
        TestSubscriber<String> subscriber = new TestSubscriber<>();
        source.subscribe(subscriber);
        scheduler.advanceTimeBy(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        assertThat(subscriber.getOnNextEvents())
                .containsExactly("A", "B");
    }

    @Test
    public void should_create_a_hot_observable_taking_in_account_frame_time_factor()
    {
        scheduler = new MarbleScheduler(100);
        HotObservable<String> source = scheduler.createHotObservable("--a---b--|", of("a", "A", "b", "B"));
        TestSubscriber<String> subscriber = new TestSubscriber<>();
        source.subscribe(subscriber);
        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);

        assertThat(subscriber.getOnNextEvents()).isEmpty();
    }

    @Test
    public void should_create_a_hot_observable_sending_events_occurring_after_subscribe()
    {
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
    public void should_parse_a_simple_time_marble_string_to_a_number()
    {
        long time = scheduler.createTime("-----|");
        assertThat(time).isEqualTo(50l);
    }
    
    @Test(expected = RuntimeException.class)
    public void Should_throw_if_not_given_good_marble_input()
    {
        scheduler.createTime("-a-b-c-#");
    }


}