package io.reactivex.marble;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class HotObservable<T> extends Observable<T> implements TestableObservable<T> {

    private final List<Recorded<T>> notifications;
    private final List<Observer<? super T>> subscribers = new ArrayList<>();
    private final Scheduler scheduler;
    List<SubscriptionLog> subscriptions = new ArrayList<>();

    protected HotObservable(Scheduler scheduler, List<Recorded<T>> notifications) {
        this.scheduler = scheduler;
        this.notifications = notifications;
        scheduleNotifications();
    }

    private void scheduleNotifications() {
        Scheduler.Worker worker = scheduler.createWorker();
        for (final Recorded<T> event : notifications) {
            worker.schedule(new Runnable() {
                @Override
                public void run() {
                for (Observer<? super T> subscriber : subscribers){
                    NotificationHelper.accept(event.value, subscriber);
                }
                }
            }, event.time, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    protected void subscribeActual(final Observer<? super T> observer) {
        subscribers.add(observer);

        final SubscriptionLog subscriptionLog = new SubscriptionLog(scheduler.now(TimeUnit.MILLISECONDS));
        subscriptions.add(subscriptionLog);
        final int subscriptionIndex = subscriptions.size() - 1;


        observer.onSubscribe(new Disposable() {
            @Override
            public void dispose() {
                subscribers.remove(observer);
                subscriptions.set(
                    subscriptionIndex,
                    new SubscriptionLog(subscriptionLog.subscribe, scheduler.now(TimeUnit.MILLISECONDS))
                );
            }

            @Override
            public boolean isDisposed() {
                return !subscribers.contains(observer);
            }
        });
    }
    @Override
    public List<SubscriptionLog> getSubscriptions() {
        return Collections.unmodifiableList(subscriptions);
    }

    @Override
    public List<Recorded<T>> getMessages() {
        return Collections.unmodifiableList(notifications);
    }

    public static <T> HotObservable<T> create(Scheduler scheduler, Recorded<T>... notifications) {
        return create(scheduler, Arrays.asList(notifications));
    }

    public static <T> HotObservable<T> create(Scheduler scheduler, List<Recorded<T>> notifications) {
        //OnSubscribeHandler<T> onSubscribeFunc = new OnSubscribeHandler<>(scheduler, notifications);
        HotObservable<T> observable = new HotObservable<>(scheduler, notifications);
        //onSubscribeFunc.observable = observable;
        return observable;
    }


    private static class OnSubscribeHandler<T> {

        private final Scheduler scheduler;
        private final List<Observer<? super T>> subscribers = new ArrayList<>();
        public HotObservable<T> observable;

        public OnSubscribeHandler(Scheduler scheduler, List<Recorded<T>> notifications) {
            this.scheduler = scheduler;
            Scheduler.Worker worker = scheduler.createWorker();
            for (final Recorded<T> event : notifications) {
                worker.schedule(new Runnable() {
                    @Override
                    public void run() {
                        List<Observer<? super T>> subscribers
                                = new ArrayList<>(HotObservable.OnSubscribeHandler.this.subscribers);
                        for (Observer<? super T> subscriber : subscribers){
                            NotificationHelper.accept(event.value, subscriber);
                        }
                    }
                }, event.time, TimeUnit.MILLISECONDS);
            }
        }

        public void call(final Observer<? super T> subscriber) {
            final SubscriptionLog subscriptionLog = new SubscriptionLog(scheduler.now(TimeUnit.MILLISECONDS));
            observable.subscriptions.add(subscriptionLog);
            final int subscriptionIndex = observable.getSubscriptions().size() - 1;

            subscribers.add(subscriber);

            /* TODO on unsubscribe
            subscriber.add((Subscriptions.create(new Action0() {
                @Override
                public void call() {
                    // on unsubscribe
                    observable.subscriptions.set(
                            subscriptionIndex,
                            new SubscriptionLog(subscriptionLog.subscribe, scheduler.now())
                    );
                    subscribers.remove(subscriber);
                }
            })));
            */
        }
    }
}
