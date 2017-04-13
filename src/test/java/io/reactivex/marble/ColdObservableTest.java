package io.reactivex.marble;

import io.reactivex.Notification;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.TestScheduler;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;


public class ColdObservableTest {


    @Test
    public void should_send_notification_on_subscribe() {
        // given
        TestScheduler scheduler = new TestScheduler();
        Recorded<String> event = new Recorded<>(0, Notification.createOnNext("Hello world!"));
        ColdObservable<String> coldObservable = ColdObservable.create(scheduler, event);
        // when
        TestObserver<String> observer = new TestObserver<>();
        coldObservable.subscribe(observer);
        // then
        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        observer.assertValue("Hello world!");
    }

    @Test
    public void should_send_notification_on_subscribe_using_offset() {
        // given
        TestScheduler scheduler = new TestScheduler();
        long offset = 10;
        Recorded<String> event = new Recorded<>(offset, Notification.createOnNext("Hello world!"));
        ColdObservable<String> coldObservable = ColdObservable.create(scheduler, event);
        // when
        TestObserver<String> observer = new TestObserver<>();
        coldObservable.subscribe(observer);
        // then
        scheduler.advanceTimeBy(9, TimeUnit.MILLISECONDS);
        observer.assertNoValues();
        scheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);
        observer.assertValue("Hello world!");
    }

    @Test
    public void should_not_send_notification_after_unsubscribe() {
        // given
        TestScheduler scheduler = new TestScheduler();
        long offset = 10;
        Recorded<String> event = new Recorded<>(offset, Notification.createOnNext("Hello world!"));
        ColdObservable<String> coldObservable = ColdObservable.create(scheduler, event);
        final TestObserver<String> observer = new TestObserver<>();
        coldObservable.subscribe(observer);
        // when
        scheduler.createWorker().schedule(new Runnable() {
            @Override
            public void run() {
                observer.dispose();
            }
        }, 5, TimeUnit.MILLISECONDS);
        // then
        scheduler.advanceTimeBy(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        observer.assertNoValues();
    }

    @Test
    public void should_be_cold_and_send_notification_at_subscribe_time() {
        // given
        TestScheduler scheduler = new TestScheduler();
        Recorded<String> event = new Recorded<>(0, Notification.createOnNext("Hello world!"));
        final ColdObservable<String> coldObservable = ColdObservable.create(scheduler, event);
        // when
        final TestObserver<String> observer = new TestObserver<>();
        scheduler.createWorker().schedule(new Runnable() {
            @Override
            public void run() {
                coldObservable.subscribe(observer);
            }
        }, 42, TimeUnit.SECONDS);
        // then
        scheduler.advanceTimeBy(42, TimeUnit.SECONDS);
        observer.assertValue("Hello world!");
    }

    @Test
    public void should_keep_track_of_subscriptions() {
        // given
        TestScheduler scheduler = new TestScheduler();
        final ColdObservable<String> coldObservable = ColdObservable.create(scheduler);
        // when
        final TestObserver<String> observer = new TestObserver<>();

        scheduler.createWorker().schedule(new Runnable() {
            @Override
            public void run() {
                coldObservable.subscribe(observer);
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
        final TestObserver<String> observer = new TestObserver<>();
        coldObservable.subscribe(observer);
        scheduler.createWorker().schedule(new Runnable() {
            @Override
            public void run() {
                observer.dispose();
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
        final TestObserver<String> observer1 = new TestObserver<>();
        final TestObserver<String> observer2 = new TestObserver<>();
        coldObservable.subscribe(observer1);
        scheduler.createWorker().schedule(new Runnable() {
            @Override
            public void run() {
                coldObservable.subscribe(observer2);
            }
        }, 36, TimeUnit.MILLISECONDS);
        scheduler.createWorker().schedule(new Runnable() {
            @Override
            public void run() {
                observer1.dispose();
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