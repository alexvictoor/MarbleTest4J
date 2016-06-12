package rx.marble;

import org.junit.Test;
import rx.Notification;
import rx.Subscription;
import rx.functions.Action0;
import rx.observers.TestSubscriber;
import rx.schedulers.TestScheduler;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;


public class ColdObservableTest {


    @Test
    public void should_send_notification_on_subscribe() {
        // given
        TestScheduler scheduler = new TestScheduler();
        Recorded<Notification<String>> event = new Recorded<>(0, Notification.createOnNext("Hello world!"));
        ColdObservable<String> coldObservable = ColdObservable.create(scheduler, event);
        // when
        TestSubscriber<String> subscriber = new TestSubscriber<>();
        coldObservable.subscribe(subscriber);
        // then
        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        assertThat(subscriber.getOnNextEvents()).containsExactly("Hello world!");
    }

    @Test
    public void should_send_notification_on_subscribe_using_offset() {
        // given
        TestScheduler scheduler = new TestScheduler();
        long offset = 10;
        Recorded<Notification<String>> event = new Recorded<>(offset, Notification.createOnNext("Hello world!"));
        ColdObservable<String> coldObservable = ColdObservable.create(scheduler, event);
        // when
        TestSubscriber<String> subscriber = new TestSubscriber<>();
        coldObservable.subscribe(subscriber);
        // then
        scheduler.advanceTimeBy(9, TimeUnit.MILLISECONDS);
        assertThat(subscriber.getOnNextEvents()).isEmpty();
        scheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);
        assertThat(subscriber.getOnNextEvents()).containsExactly("Hello world!");
    }

    @Test
    public void should_not_send_notification_after_unsubscribe() {
        // given
        TestScheduler scheduler = new TestScheduler();
        long offset = 10;
        Recorded<Notification<String>> event = new Recorded<>(offset, Notification.createOnNext("Hello world!"));
        ColdObservable<String> coldObservable = ColdObservable.create(scheduler, event);
        TestSubscriber<String> subscriber = new TestSubscriber<>();
        final Subscription subscription = coldObservable.subscribe(subscriber);
        // when
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
    public void should_be_cold_and_send_notification_at_subscribe_time() {
        // given
        TestScheduler scheduler = new TestScheduler();
        Recorded<Notification<String>> event = new Recorded<>(0, Notification.createOnNext("Hello world!"));
        final ColdObservable<String> coldObservable = ColdObservable.create(scheduler, event);
        // when
        final TestSubscriber<String> subscriber = new TestSubscriber<>();
        scheduler.createWorker().schedule(new Action0() {
            @Override
            public void call() {
                coldObservable.subscribe(subscriber);
            }
        }, 42, TimeUnit.SECONDS);
        // then
        scheduler.advanceTimeBy(42, TimeUnit.SECONDS);
        assertThat(subscriber.getOnNextEvents()).containsExactly("Hello world!");
    }

    @Test
    public void should_keep_track_of_subscriptions() {
        // given
        TestScheduler scheduler = new TestScheduler();
        final ColdObservable<String> coldObservable = ColdObservable.create(scheduler);
        // when
        final TestSubscriber<String> subscriber = new TestSubscriber<>();

        scheduler.createWorker().schedule(new Action0() {
            @Override
            public void call() {
                coldObservable.subscribe(subscriber);
            }
        }, 42, TimeUnit.MILLISECONDS);
        // then
        scheduler.advanceTimeBy(42, TimeUnit.MILLISECONDS);
        assertThat(coldObservable.getSubscriptions())
                .containsExactly(
                        new SubscriptionLog(42, Long.MAX_VALUE)
                );
    }

    @Test
    public void should_keep_track_of_unsubscriptions() {
        // given
        TestScheduler scheduler = new TestScheduler();
        final ColdObservable<String> coldObservable = ColdObservable.create(scheduler);
        // when
        final TestSubscriber<String> subscriber = new TestSubscriber<>();
        final Subscription subscription = coldObservable.subscribe(subscriber);
        scheduler.createWorker().schedule(new Action0() {
            @Override
            public void call() {
                subscription.unsubscribe();
            }
        }, 42, TimeUnit.MILLISECONDS);
        // then
        scheduler.advanceTimeBy(42, TimeUnit.MILLISECONDS);
        assertThat(coldObservable.getSubscriptions())
                .containsExactly(
                        new SubscriptionLog(0, 42)
                );
    }

    @Test
    public void should_keep_track_of_several_subscriptions() {
        // given
        TestScheduler scheduler = new TestScheduler();
        final ColdObservable<String> coldObservable = ColdObservable.create(scheduler);
        // when
        final TestSubscriber<String> subscriber1 = new TestSubscriber<>();
        final TestSubscriber<String> subscriber2 = new TestSubscriber<>();
        final Subscription subscription = coldObservable.subscribe(subscriber1);
        scheduler.createWorker().schedule(new Action0() {
            @Override
            public void call() {
                coldObservable.subscribe(subscriber2);
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
        assertThat(coldObservable.getSubscriptions())
                .containsExactly(
                        new SubscriptionLog(0, 42),
                        new SubscriptionLog(36, Long.MAX_VALUE)
                );
    }
}