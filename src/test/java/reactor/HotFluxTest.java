package reactor;

import io.reactivex.subscribers.TestSubscriber;
import org.junit.Test;
import org.reactivestreams.Notification;
import org.reactivestreams.Recorded;
import org.reactivestreams.SubscriptionLog;
import reactor.core.publisher.Flux;
import reactor.test.scheduler.VirtualTimeScheduler;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;


public class HotFluxTest {

    @Test
    public void should_send_notification_occurring_after_subscribe() {
        // given
        VirtualTimeScheduler scheduler = VirtualTimeScheduler.create();
        Recorded<String> event = new Recorded<>(10, Notification.createOnNext("Hello world!"));
        Flux<String> hotFlux = HotFlux.create(scheduler, event);
        // when
        TestSubscriber<String> subscriber = new TestSubscriber<>();
        hotFlux.subscribe(subscriber);
        // then
        scheduler.advanceTimeBy(Duration.ofMillis(10));
        subscriber.assertValue("Hello world!");
    }

    @Test
    public void should_not_send_notification_occurring_before_subscribe() {
        // given
        VirtualTimeScheduler scheduler = VirtualTimeScheduler.create();
        Recorded<String> event = new Recorded<>(10, Notification.createOnNext("Hello world!"));
        final Flux<String> hotFlux = HotFlux.create(scheduler, event);
        // when
        final TestSubscriber<String> subscriber = new TestSubscriber<>();
        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                hotFlux.subscribe(subscriber);
            }
        }, 15, TimeUnit.MILLISECONDS);
        // then
        scheduler.advanceTimeBy(Duration.ofNanos(Long.MAX_VALUE));
        subscriber.assertNoValues();
    }

    @Test
    public void should_not_send_notification_occurring_after_unsubscribe() {
        // given
        VirtualTimeScheduler scheduler = VirtualTimeScheduler.create();
        Recorded<String> event = new Recorded<>(10, Notification.createOnNext("Hello world!"));
        final Flux<String> hotFlux = HotFlux.create(scheduler, event);
        // when
        final TestSubscriber<String> subscriber = new TestSubscriber<>();
        hotFlux.subscribe(subscriber);

        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                subscriber.dispose();
            }
        }, 5, TimeUnit.MILLISECONDS);
        // then
        scheduler.advanceTimeBy(Duration.ofNanos(Long.MAX_VALUE));
        subscriber.assertNoValues();
    }

    @Test
    public void should_keep_track_of_subscriptions() {
        // given
        VirtualTimeScheduler scheduler = VirtualTimeScheduler.create();
        final HotFlux<String> hotFlux = HotFlux.create(scheduler);
        // when
        final TestSubscriber<String> subscriber = new TestSubscriber<>();

        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                hotFlux.subscribe(subscriber);
            }
        }, 42, TimeUnit.MILLISECONDS);
        // then
        scheduler.advanceTimeBy(Duration.ofMillis(42));
        assertThat(hotFlux.getSubscriptions())
                .containsExactly(
                        new SubscriptionLog(42, Long.MAX_VALUE)
                );
    }

    @Test
    public void should_keep_track_of_unsubscriptions() {
        // given
        VirtualTimeScheduler scheduler = VirtualTimeScheduler.create();
        final HotFlux<String> hotFlux = HotFlux.create(scheduler);
        // when
        final TestSubscriber<String> subscriber = new TestSubscriber<>();
        hotFlux.subscribe(subscriber);
        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                subscriber.dispose();
            }
        }, 42, TimeUnit.MILLISECONDS);
        // then
        scheduler.advanceTimeBy(Duration.ofMillis(42));
        assertThat(hotFlux.getSubscriptions())
                .containsExactly(
                        new SubscriptionLog(0, 42)
                );
    }

    @Test
    public void should_keep_track_of_several_subscriptions() {
        // given
        VirtualTimeScheduler scheduler = VirtualTimeScheduler.create();
        final HotFlux<String> hotFlux = HotFlux.create(scheduler);
        // when
        final TestSubscriber<String> subscriber1 = new TestSubscriber<>();
        final TestSubscriber<String> subscriber2 = new TestSubscriber<>();
        hotFlux.subscribe(subscriber1);
        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                hotFlux.subscribe(subscriber2);
            }
        }, 36, TimeUnit.MILLISECONDS);
        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                subscriber1.dispose();
            }
        }, 42, TimeUnit.MILLISECONDS);
        // then
        scheduler.advanceTimeBy(Duration.ofMillis(42));
        assertThat(hotFlux.getSubscriptions())
                .containsExactly(
                        new SubscriptionLog(0, 42),
                        new SubscriptionLog(36, Long.MAX_VALUE)
                );
    }

}