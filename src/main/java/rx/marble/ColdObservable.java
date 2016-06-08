package rx.marble;

import rx.Notification;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by Alexandre Victoor on 08/06/2016.
 */
public class ColdObservable<T> extends Observable<T> {

    public final Recorded<Notification<T>>[] notifications;
    protected List<SubscriptionLog> subscriptions = new ArrayList<>();

    protected ColdObservable(OnSubscribe<T> f, Recorded<Notification<T>>[] notifications) {
        super(f);
        this.notifications = notifications;
    }

    public static <T> ColdObservable<T> create(Scheduler scheduler, Recorded<Notification<T>>... notifications) {
        CustomOnSubscribe<T> onSubscribeFunc = new CustomOnSubscribe<>(scheduler, notifications);
        ColdObservable<T> observable = new ColdObservable<>(onSubscribeFunc, notifications);
        onSubscribeFunc.observable = observable;
        return observable;
    }


    public static class CustomOnSubscribe<T> implements Observable.OnSubscribe<T> {

        private final Scheduler scheduler;
        private final Recorded<Notification<T>>[] notifications;
        public ColdObservable<T> observable;

        public CustomOnSubscribe(Scheduler scheduler, Recorded<Notification<T>>[] notifications) {
            this.scheduler = scheduler;
            this.notifications = notifications;
        }

        public void call(final Subscriber<? super T> subscriber) {
            final SubscriptionLog subscriptionLog = new SubscriptionLog(scheduler.now());
            observable.subscriptions.add(subscriptionLog);
            final int subscriptionIndex = observable.subscriptions.size() - 1;
            Scheduler.Worker worker = scheduler.createWorker();
            subscriber.add(worker);

            for (final Recorded<Notification<T>> notification: notifications) {
                worker.schedule(new Action0() {
                    @Override
                    public void call() {
                        notification.value.accept(subscriber);
                    }
                }, notification.time, TimeUnit.NANOSECONDS);
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
