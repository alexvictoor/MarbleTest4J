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


public class ColdObservable<T> extends Observable<T> implements TestableObservable<T> {

    private final Scheduler scheduler;
    private final List<Recorded<T>> recordedNotifications;
    private final List<SubscriptionLog> subscriptions = new ArrayList<>();

    public ColdObservable(Scheduler scheduler, List<Recorded<T>> notifications) {
        this.scheduler = scheduler;
        this.recordedNotifications = notifications;
    }

    @Override
    protected void subscribeActual(final Observer<? super T> observer) {
        final SubscriptionLog subscriptionLog = new SubscriptionLog(scheduler.now(TimeUnit.MILLISECONDS));
        subscriptions.add(subscriptionLog);
        final int subscriptionIndex = subscriptions.size() - 1;
        final Scheduler.Worker worker = scheduler.createWorker();
        for (final Recorded<T> event: recordedNotifications) {
            worker.schedule(new Runnable() {
                @Override
                public void run() {
                    NotificationHelper.accept(event.value, observer);
                    if (!event.value.isOnNext()) {
                        endSubscriptions(event.time);
                    }
                }
            }, event.time, TimeUnit.MILLISECONDS);
        }

        observer.onSubscribe(new Disposable() {

            private boolean disposed = false;

            @Override
            public void dispose() {
                disposed = true;
                subscriptions.set(
                        subscriptionIndex,
                        new SubscriptionLog(subscriptionLog.subscribe, scheduler.now(TimeUnit.MILLISECONDS))
                );
                worker.dispose();
            }

            @Override
            public boolean isDisposed() {
                return disposed;
            }
        });
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
    public List<SubscriptionLog> getSubscriptions() {
        return Collections.unmodifiableList(subscriptions);
    }

    @Override
    public List<Recorded<T>> getMessages() {
        return Collections.unmodifiableList(recordedNotifications);
    }

    public static <T> ColdObservable<T> create(Scheduler scheduler, Recorded<T>... notifications) {
        return create(scheduler, Arrays.asList(notifications));
    }

    public static <T> ColdObservable<T> create(Scheduler scheduler, List<Recorded<T>> notifications) {
        ColdObservable<T> observable = new ColdObservable<>(scheduler, notifications);
        return observable;
    }

}
