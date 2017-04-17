package reactor;

import io.reactivex.subscribers.TestSubscriber;
import org.junit.Test;
import reactor.core.publisher.Signal;
import reactor.test.scheduler.VirtualTimeScheduler;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;


public class HotObservableTest {

    @Test
    public void should_send_notification_occurring_after_subscribe() {
        // given
        VirtualTimeScheduler scheduler = VirtualTimeScheduler.create();
        Recorded<String> event = new Recorded<>(10, Signal.next("Hello world!"));
        HotObservable<String> hotObservable = HotObservable.create(scheduler, event);
        // when
        TestSubscriber<String> observer = new TestSubscriber<>();
        hotObservable.subscribe(observer);
        // then
        scheduler.advanceTimeBy(Duration.ofMillis(10));
        observer.assertValue("Hello world!");
    }

    @Test
    public void should_not_send_notification_occurring_before_subscribe() {
        // given
        VirtualTimeScheduler scheduler = VirtualTimeScheduler.create();
        Recorded<String> event = new Recorded<>(10, Signal.next("Hello world!"));
        final HotObservable<String> hotObservable = HotObservable.create(scheduler, event);
        // when
        final TestSubscriber<String> observer = new TestSubscriber<>();
        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                hotObservable.subscribe(observer);
            }
        }, 15, TimeUnit.MILLISECONDS);
        // then
        scheduler.advanceTimeBy(Duration.ofNanos(Long.MAX_VALUE));
        observer.assertNoValues();
    }

    @Test
    public void should_not_send_notification_occurring_after_unsubscribe() {
        // given
        VirtualTimeScheduler scheduler = VirtualTimeScheduler.create();
        Recorded<String> event = new Recorded<>(10, Signal.next("Hello world!"));
        final HotObservable<String> hotObservable = HotObservable.create(scheduler, event);
        // when
        final TestSubscriber<String> observer = new TestSubscriber<>();
        hotObservable.subscribe(observer);

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
    public void should_keep_track_of_subscriptions() {
        // given
        VirtualTimeScheduler scheduler = VirtualTimeScheduler.create();
        final HotObservable<String> hotObservable = HotObservable.create(scheduler);
        // when
        final TestSubscriber<String> observer = new TestSubscriber<>();

        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                hotObservable.subscribe(observer);
            }
        }, 42, TimeUnit.MILLISECONDS);
        // then
        scheduler.advanceTimeBy(Duration.ofMillis(42));
        assertThat(hotObservable.subscriptions)
                .containsExactly(
                        new SubscriptionLog(42, Long.MAX_VALUE)
                );
    }

    @Test
    public void should_keep_track_of_unsubscriptions() {
        // given
        VirtualTimeScheduler scheduler = VirtualTimeScheduler.create();
        final HotObservable<String> hotObservable = HotObservable.create(scheduler);
        // when
        final TestSubscriber<String> observer = new TestSubscriber<>();
        hotObservable.subscribe(observer);
        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                observer.dispose();
            }
        }, 42, TimeUnit.MILLISECONDS);
        // then
        scheduler.advanceTimeBy(Duration.ofMillis(42));
        assertThat(hotObservable.subscriptions)
                .containsExactly(
                        new SubscriptionLog(0, 42)
                );
    }

    @Test
    public void should_keep_track_of_several_subscriptions() {
        // given
        VirtualTimeScheduler scheduler = VirtualTimeScheduler.create();
        final HotObservable<String> hotObservable = HotObservable.create(scheduler);
        // when
        final TestSubscriber<String> observer1 = new TestSubscriber<>();
        final TestSubscriber<String> observer2 = new TestSubscriber<>();
        hotObservable.subscribe(observer1);
        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                hotObservable.subscribe(observer2);
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
        assertThat(hotObservable.subscriptions)
                .containsExactly(
                        new SubscriptionLog(0, 42),
                        new SubscriptionLog(36, Long.MAX_VALUE)
                );
    }

}