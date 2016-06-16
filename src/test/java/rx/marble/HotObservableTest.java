package rx.marble;

import org.junit.Test;
import rx.Notification;
import rx.Subscription;
import rx.functions.Action0;
import rx.observers.TestSubscriber;
import rx.schedulers.TestScheduler;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;


public class HotObservableTest {

    @Test
    public void should_send_notification_occurring_after_subscribe() {
        // given
        TestScheduler scheduler = new TestScheduler();
        Recorded<Notification<String>> event = new Recorded<>(10, Notification.createOnNext("Hello world!"));
        HotObservable<String> hotObservable = HotObservable.create(scheduler, event);
        // when
        TestSubscriber<String> subscriber = new TestSubscriber<>();
        hotObservable.subscribe(subscriber);
        // then
        scheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        assertThat(subscriber.getOnNextEvents()).containsExactly("Hello world!");
    }

    @Test
    public void should_not_send_notification_occurring_before_subscribe() {
        // given
        TestScheduler scheduler = new TestScheduler();
        Recorded<Notification<String>> event = new Recorded<>(10, Notification.createOnNext("Hello world!"));
        final HotObservable<String> hotObservable = HotObservable.create(scheduler, event);
        // when
        final TestSubscriber<String> subscriber = new TestSubscriber<>();
        scheduler.createWorker().schedule(new Action0() {
            @Override
            public void call() {
                hotObservable.subscribe(subscriber);
            }
        }, 15, TimeUnit.MILLISECONDS);
        // then
        scheduler.advanceTimeBy(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        assertThat(subscriber.getOnNextEvents()).isEmpty();
    }

    @Test
    public void should_not_send_notification_occurring_after_unsubscribe() {
        // given
        TestScheduler scheduler = new TestScheduler();
        Recorded<Notification<String>> event = new Recorded<>(10, Notification.createOnNext("Hello world!"));
        final HotObservable<String> hotObservable = HotObservable.create(scheduler, event);
        // when
        final TestSubscriber<String> subscriber = new TestSubscriber<>();
        final Subscription subscription = hotObservable.subscribe(subscriber);
        scheduler.createWorker().schedule(new Action0() {
            @Override
            public void call() {
                subscription.unsubscribe();
            }
        }, 5, TimeUnit.MILLISECONDS);
        // then
        scheduler.advanceTimeBy(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        assertThat(subscriber.getOnNextEvents()).isEmpty();
    }

    @Test
    public void should_keep_track_of_subscriptions() {
        // given
        TestScheduler scheduler = new TestScheduler();
        final HotObservable<String> hotObservable = HotObservable.create(scheduler);
        // when
        final TestSubscriber<String> subscriber = new TestSubscriber<>();

        scheduler.createWorker().schedule(new Action0() {
            @Override
            public void call() {
                hotObservable.subscribe(subscriber);
            }
        }, 42, TimeUnit.MILLISECONDS);
        // then
        scheduler.advanceTimeBy(42, TimeUnit.MILLISECONDS);
        assertThat(hotObservable.subscriptions)
                .containsExactly(
                        new SubscriptionLog(42, Long.MAX_VALUE)
                );
    }

    @Test
    public void should_keep_track_of_unsubscriptions() {
        // given
        TestScheduler scheduler = new TestScheduler();
        final HotObservable<String> hotObservable = HotObservable.create(scheduler);
        // when
        final TestSubscriber<String> subscriber = new TestSubscriber<>();
        final Subscription subscription = hotObservable.subscribe(subscriber);
        scheduler.createWorker().schedule(new Action0() {
            @Override
            public void call() {
                subscription.unsubscribe();
            }
        }, 42, TimeUnit.MILLISECONDS);
        // then
        scheduler.advanceTimeBy(42, TimeUnit.MILLISECONDS);
        assertThat(hotObservable.subscriptions)
                .containsExactly(
                        new SubscriptionLog(0, 42)
                );
    }

    @Test
    public void should_keep_track_of_several_subscriptions() {
        // given
        TestScheduler scheduler = new TestScheduler();
        final HotObservable<String> hotObservable = HotObservable.create(scheduler);
        // when
        final TestSubscriber<String> subscriber1 = new TestSubscriber<>();
        final TestSubscriber<String> subscriber2 = new TestSubscriber<>();
        final Subscription subscription = hotObservable.subscribe(subscriber1);
        scheduler.createWorker().schedule(new Action0() {
            @Override
            public void call() {
                hotObservable.subscribe(subscriber2);
            }
        }, 36, TimeUnit.MILLISECONDS);
        scheduler.createWorker().schedule(new Action0() {
            @Override
            public void call() {
                subscription.unsubscribe();
            }
        }, 42, TimeUnit.MILLISECONDS);
        // then
        scheduler.advanceTimeBy(42, TimeUnit.MILLISECONDS);
        assertThat(hotObservable.subscriptions)
                .containsExactly(
                        new SubscriptionLog(0, 42),
                        new SubscriptionLog(36, Long.MAX_VALUE)
                );
    }

}