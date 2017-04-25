package io.reactivex.marble;

import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.TestScheduler;
import org.junit.Test;
import org.reactivestreams.Notification;
import org.reactivestreams.Recorded;
import org.reactivestreams.SubscriptionLog;

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
        TestObserver<String> observer = new TestObserver<>();
        hotObservable.subscribe(observer);
        // then
        scheduler.advanceTimeBy(10, TimeUnit.SECONDS);
        observer.assertValue("Hello world!");
    }

    @Test
    public void should_not_send_notification_occurring_before_subscribe() {
        // given
        TestScheduler scheduler = new TestScheduler();
        Recorded<String> event = new Recorded<>(10, Notification.createOnNext("Hello world!"));
        final HotObservable<String> hotObservable = HotObservable.create(scheduler, event);
        // when
        final TestObserver<String> observer = new TestObserver<>();
        scheduler.createWorker().schedule(new Runnable() {
            @Override
            public void run() {
                hotObservable.subscribe(observer);
            }
        }, 15, TimeUnit.MILLISECONDS);
        // then
        scheduler.advanceTimeBy(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        observer.assertNoValues();
    }

    @Test
    public void should_not_send_notification_occurring_after_unsubscribe() {
        // given
        TestScheduler scheduler = new TestScheduler();
        Recorded<String> event = new Recorded<>(10, Notification.createOnNext("Hello world!"));
        final HotObservable<String> hotObservable = HotObservable.create(scheduler, event);
        // when
        final TestObserver<String> observer = new TestObserver<>();
        hotObservable.subscribe(observer);

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
    public void should_keep_track_of_subscriptions() {
        // given
        TestScheduler scheduler = new TestScheduler();
        final HotObservable<String> hotObservable = HotObservable.create(scheduler);
        // when
        final TestObserver<String> observer = new TestObserver<>();

        scheduler.createWorker().schedule(new Runnable() {
            @Override
            public void run() {
                hotObservable.subscribe(observer);
            }
        }, 42, TimeUnit.MILLISECONDS);
        // then
        scheduler.advanceTimeBy(42, TimeUnit.MILLISECONDS);
        assertThat(hotObservable.getSubscriptions())
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
        final TestObserver<String> observer = new TestObserver<>();
        hotObservable.subscribe(observer);
        scheduler.createWorker().schedule(new Runnable() {
            @Override
            public void run() {
                observer.dispose();
            }
        }, 42, TimeUnit.MILLISECONDS);
        // then
        scheduler.advanceTimeBy(42, TimeUnit.MILLISECONDS);
        assertThat(hotObservable.getSubscriptions())
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
        final TestObserver<String> observer1 = new TestObserver<>();
        final TestObserver<String> observer2 = new TestObserver<>();
        hotObservable.subscribe(observer1);
        scheduler.createWorker().schedule(new Runnable() {
            @Override
            public void run() {
                hotObservable.subscribe(observer2);
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
        assertThat(hotObservable.getSubscriptions())
                .containsExactly(
                        new SubscriptionLog(0, 42),
                        new SubscriptionLog(36, Long.MAX_VALUE)
                );
    }

}