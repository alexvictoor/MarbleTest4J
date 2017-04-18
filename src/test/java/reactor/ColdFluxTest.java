package reactor;

import io.reactivex.subscribers.TestSubscriber;
import org.junit.Test;
import org.reactivestreams.Notification;
import org.reactivestreams.Recorded;
import org.reactivestreams.SubscriptionLog;
import reactor.test.scheduler.VirtualTimeScheduler;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;


public class ColdFluxTest {


    @Test
    public void should_send_notification_on_subscribe() {
        // given
        VirtualTimeScheduler scheduler = VirtualTimeScheduler.create();
        Recorded<String> event = new Recorded<>(0, Notification.createOnNext("Hello world!"));
        ColdFlux<String> coldObservable = ColdFlux.create(scheduler, event);
        // when
        TestSubscriber<String> observer = new TestSubscriber<>();
        coldObservable.subscribe(observer);
        // then
        scheduler.advanceTimeBy(Duration.ofSeconds(1));
        observer.assertValue("Hello world!");
    }

    @Test
    public void should_send_notification_on_subscribe_using_offset() {
        // given
        VirtualTimeScheduler scheduler = VirtualTimeScheduler.create();
        long offset = 10;
        Recorded<String> event = new Recorded<>(offset, Notification.createOnNext("Hello world!"));
        ColdFlux<String> coldObservable = ColdFlux.create(scheduler, event);
        // when
        TestSubscriber<String> observer = new TestSubscriber<>();
        coldObservable.subscribe(observer);
        // then
        scheduler.advanceTimeBy(Duration.ofMillis(9));
        observer.assertNoValues();
        scheduler.advanceTimeBy(Duration.ofMillis(1));
        observer.assertValue("Hello world!");
    }

    @Test
    public void should_not_send_notification_after_unsubscribe() {
        // given
        VirtualTimeScheduler scheduler = VirtualTimeScheduler.create();
        long offset = 10;
        Recorded<String> event = new Recorded<>(offset, Notification.createOnNext("Hello world!"));
        ColdFlux<String> coldObservable = ColdFlux.create(scheduler, event);
        final TestSubscriber<String> observer = new TestSubscriber<>();
        coldObservable.subscribe(observer);
        // when
        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                observer.dispose();
            }
        }, 5, TimeUnit.MILLISECONDS);
        // then
        scheduler.advanceTimeBy(Duration.ofNanos(Long.MAX_VALUE));
        observer.assertNoValues();
    }

    @Test
    public void should_be_cold_and_send_notification_at_subscribe_time() {
        // given
        VirtualTimeScheduler scheduler = VirtualTimeScheduler.create();
        Recorded<String> event = new Recorded<>(0, Notification.createOnNext("Hello world!"));
        final ColdFlux<String> coldObservable = ColdFlux.create(scheduler, event);
        // when
        final TestSubscriber<String> observer = new TestSubscriber<>();
        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                coldObservable.subscribe(observer);
            }
        }, 42, TimeUnit.SECONDS);
        // then
        scheduler.advanceTimeBy(Duration.ofSeconds(42));
        observer.assertValue("Hello world!");
    }

    @Test
    public void should_keep_track_of_subscriptions() {
        // given
        VirtualTimeScheduler scheduler = VirtualTimeScheduler.create();
        final ColdFlux<String> coldObservable = ColdFlux.create(scheduler);
        // when
        final TestSubscriber<String> observer = new TestSubscriber<>();

        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                coldObservable.subscribe(observer);
            }
        }, 42, TimeUnit.MILLISECONDS);
        // then
        scheduler.advanceTimeBy(Duration.ofMillis(42));
        assertThat(coldObservable.getSubscriptions())
                .containsExactly(
                        new SubscriptionLog(42, Long.MAX_VALUE)
                );
    }

    @Test
    public void should_keep_track_of_unsubscriptions() {
        // given
        VirtualTimeScheduler scheduler = VirtualTimeScheduler.create();
        final ColdFlux<String> coldObservable = ColdFlux.create(scheduler);
        // when
        final TestSubscriber<String> observer = new TestSubscriber<>();
        coldObservable.subscribe(observer);
        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                observer.dispose();
            }
        }, 42, TimeUnit.MILLISECONDS);
        // then
        scheduler.advanceTimeBy(Duration.ofMillis(42));
        assertThat(coldObservable.getSubscriptions())
                .containsExactly(
                        new SubscriptionLog(0, 42)
                );
    }

    @Test
    public void should_keep_track_of_several_subscriptions() {
        // given
        VirtualTimeScheduler scheduler = VirtualTimeScheduler.create();
        final ColdFlux<String> coldObservable = ColdFlux.create(scheduler);
        // when
        final TestSubscriber<String> observer1 = new TestSubscriber<>();
        final TestSubscriber<String> observer2 = new TestSubscriber<>();
        coldObservable.subscribe(observer1);
        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                coldObservable.subscribe(observer2);
            }
        }, 36, TimeUnit.MILLISECONDS);
        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                observer1.dispose();
            }
        }, 42, TimeUnit.MILLISECONDS);
        // then
        scheduler.advanceTimeBy(Duration.ofMillis(42));
        assertThat(coldObservable.getSubscriptions())
                .containsExactly(
                        new SubscriptionLog(0, 42),
                        new SubscriptionLog(36, Long.MAX_VALUE)
                );
    }
}