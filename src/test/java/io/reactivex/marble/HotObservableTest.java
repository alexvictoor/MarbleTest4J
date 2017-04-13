package io.reactivex.marble;

import io.reactivex.Notification;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.TestScheduler;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;


public class HotObservableTest {

    @Test
    public void should_send_notification_occurring_after_subscribe() {
        // given
        TestScheduler scheduler = new TestScheduler();
        Recorded<String> event = new Recorded<>(10, Notification.createOnNext("Hello world!"));
        HotObservable<String> hotObservable = HotObservable.create(scheduler, event);
        // when
        TestObserver<String> subscriber = new TestObserver<>();
        hotObservable.subscribe(subscriber);
        // then
        scheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        subscriber.assertValue("Hello world!");
    }

    @Test
    public void should_not_send_notification_occurring_before_subscribe() {
        // given
        TestScheduler scheduler = new TestScheduler();
        Recorded<String> event = new Recorded<>(10, Notification.createOnNext("Hello world!"));
        final HotObservable<String> hotObservable = HotObservable.create(scheduler, event);
        // when
        final TestObserver<String> subscriber = new TestObserver<>();
        scheduler.createWorker().schedule(new Runnable() {
            @Override
            public void run() {
                hotObservable.subscribe(subscriber);
            }
        }, 15, TimeUnit.MILLISECONDS);
        // then
        scheduler.advanceTimeBy(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        subscriber.assertNoValues();
    }

    @Test
    public void should_not_send_notification_occurring_after_unsubscribe() {
        // given
        TestScheduler scheduler = new TestScheduler();
        Recorded<String> event = new Recorded<>(10, Notification.createOnNext("Hello world!"));
        final HotObservable<String> hotObservable = HotObservable.create(scheduler, event);
        // when
        final TestObserver<String> subscriber = new TestObserver<>();
        hotObservable.subscribe(subscriber);

        scheduler.createWorker().schedule(new Runnable() {
            @Override
            public void run() {
                subscriber.dispose();
            }
        }, 5, TimeUnit.MILLISECONDS);
        // then
        scheduler.advanceTimeBy(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        subscriber.assertNoValues();
    }

    @Test
    public void should_keep_track_of_subscriptions() {
        // given
        TestScheduler scheduler = new TestScheduler();
        final HotObservable<String> hotObservable = HotObservable.create(scheduler);
        // when
        final TestObserver<String> subscriber = new TestObserver<>();

        scheduler.createWorker().schedule(new Runnable() {
            @Override
            public void run() {
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
        final TestObserver<String> subscriber = new TestObserver<>();
        hotObservable.subscribe(subscriber);
        scheduler.createWorker().schedule(new Runnable() {
            @Override
            public void run() {
                subscriber.dispose();
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
        final TestObserver<String> subscriber1 = new TestObserver<>();
        final TestObserver<String> subscriber2 = new TestObserver<>();
        hotObservable.subscribe(subscriber1);
        scheduler.createWorker().schedule(new Runnable() {
            @Override
            public void run() {
                hotObservable.subscribe(subscriber2);
            }
        }, 36, TimeUnit.MILLISECONDS);
        scheduler.createWorker().schedule(new Runnable() {
            @Override
            public void run() {
                subscriber1.dispose();
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