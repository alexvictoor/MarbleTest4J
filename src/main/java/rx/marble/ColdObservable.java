package rx.marble;

import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class ColdObservable<T> extends Observable<T> implements TestableObservable<T> {

    private final List<Recorded<T>> notifications;
    private List<SubscriptionLog> subscriptions = new ArrayList<>();

    private ColdObservable(OnSubscribe<T> f, List<Recorded<T>> notifications) {
        super(f);
        this.notifications = notifications;
    }

    @Override
    public List<SubscriptionLog> getSubscriptions() {
        return Collections.unmodifiableList(subscriptions);
    }

    @Override
    public List<Recorded<T>> getMessages() {
        return Collections.unmodifiableList(notifications);
    }

    public static <T> ColdObservable<T> create(Scheduler scheduler, Recorded<T>... notifications) {
        return create(scheduler, Arrays.asList(notifications));
    }

    public static <T> ColdObservable<T> create(Scheduler scheduler, List<Recorded<T>> notifications) {
        OnSubscribeHandler<T> onSubscribeFunc = new OnSubscribeHandler<>(scheduler, notifications);
        ColdObservable<T> observable = new ColdObservable<>(onSubscribeFunc, notifications);
        onSubscribeFunc.observable = observable;
        return observable;
    }


    private static class OnSubscribeHandler<T> implements Observable.OnSubscribe<T> {

        private final Scheduler scheduler;
        private final List<Recorded<T>> notifications;
        public ColdObservable observable;

        public OnSubscribeHandler(Scheduler scheduler, List<Recorded<T>> notifications) {
            this.scheduler = scheduler;
            this.notifications = notifications;
        }

        public void call(final Subscriber<? super T> subscriber) {
            final SubscriptionLog subscriptionLog = new SubscriptionLog(scheduler.now());
            observable.subscriptions.add(subscriptionLog);
            final int subscriptionIndex = observable.getSubscriptions().size() - 1;
            Scheduler.Worker worker = scheduler.createWorker();
            subscriber.add(worker); // not scheduling after unsubscribe

            for (final Recorded<T> notification: notifications) {
                worker.schedule(new Action0() {
                    @Override
                    public void call() {
                        notification.value.accept(subscriber);
                    }
                }, notification.time, TimeUnit.MILLISECONDS);
            }

            subscriber.add((Subscriptions.create(new Action0() {
                @Override
                public void call() {
                    // on unsubscribe
                    observable.subscriptions.set(
                            subscriptionIndex,
                            new SubscriptionLog(subscriptionLog.subscribe, scheduler.now())
                    );
                }
            })));
        }
    }
}
