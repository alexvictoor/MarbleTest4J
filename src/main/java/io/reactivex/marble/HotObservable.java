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
    private final List<Observer<? super T>> observers = new ArrayList<>();
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
                for (Observer<? super T> observer : observers){
                    NotificationHelper.accept(event.value, observer);
                    if (!event.value.isOnNext()) {
                        endSubscriptions(event.time);
                    }
                }
                }
            }, event.time, TimeUnit.MILLISECONDS);
        }
    }

    private void endSubscriptions(long time) {
        for (int i = 0; i < subscriptions.size(); i++) {
            SubscriptionLog subscription = subscriptions.get(i);
            if (subscription.doesNeverEnd()) {
                subscriptions.set(i, new SubscriptionLog(subscription.subscribe, time));
            }
        }
    }

    @Override
    protected void subscribeActual(final Observer<? super T> observer) {
        observers.add(observer);

        final SubscriptionLog subscriptionLog = new SubscriptionLog(scheduler.now(TimeUnit.MILLISECONDS));
        subscriptions.add(subscriptionLog);
        final int subscriptionIndex = subscriptions.size() - 1;


        observer.onSubscribe(new Disposable() {
            @Override
            public void dispose() {
                observers.remove(observer);
                subscriptions.set(
                    subscriptionIndex,
                    new SubscriptionLog(subscriptionLog.subscribe, scheduler.now(TimeUnit.MILLISECONDS))
                );
            }

            @Override
            public boolean isDisposed() {
                return !observers.contains(observer);
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
        HotObservable<T> observable = new HotObservable<>(scheduler, notifications);
        return observable;
    }
}
