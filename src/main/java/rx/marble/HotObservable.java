package rx.marble;

import rx.Notification;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class HotObservable<T> extends Observable<T> implements TestableObservable<T> {

    public final Recorded<Notification<T>>[] notifications;
    public List<SubscriptionLog> subscriptions = new ArrayList<>();

    protected HotObservable(OnSubscribe<T> f, Recorded<Notification<T>>[] notifications) {
        super(f);
        this.notifications = notifications;
    }

    @Override
    public List<SubscriptionLog> getSubscriptions() {
        return subscriptions;
    }

    @Override
    public List<Recorded<Notification<T>>> getMessages() {
        return Arrays.asList(notifications);
    }

    public static <T> HotObservable<T> create(Scheduler scheduler, Recorded<Notification<T>>... notifications) {
        OnSubscribeHandler<T> onSubscribeFunc = new OnSubscribeHandler<>(scheduler, notifications);
        HotObservable<T> observable = new HotObservable<>(onSubscribeFunc, notifications);
        onSubscribeFunc.observable = observable;
        return observable;
    }

    private static class OnSubscribeHandler<T> implements Observable.OnSubscribe<T> {

        private final Scheduler scheduler;
        private final List<Subscriber<? super T>> subscribers = new ArrayList<>();
        public TestableObservable<T> observable;

        public OnSubscribeHandler(Scheduler scheduler, Recorded<Notification<T>>[] notifications) {
            this.scheduler = scheduler;
            Scheduler.Worker worker = scheduler.createWorker();
            for (final Recorded<Notification<T>> event : notifications) {
                worker.schedule(new Action0() {
                    @Override
                    public void call() {
                        for (Subscriber<? super T> subscriber : subscribers){
                            event.value.accept(subscriber);
                        }
                    }
                }, event.time, TimeUnit.MILLISECONDS);
            }
        }

        public void call(final Subscriber<? super T> subscriber) {
            final SubscriptionLog subscriptionLog = new SubscriptionLog(scheduler.now());
            observable.getSubscriptions().add(subscriptionLog);
            final int subscriptionIndex = observable.getSubscriptions().size() - 1;

            subscribers.add(subscriber);

            subscriber.add((Subscriptions.create(new Action0() {
                @Override
                public void call() {
                    // on unsubscribe
                    observable.getSubscriptions().set(
                            subscriptionIndex,
                            new SubscriptionLog(subscriptionLog.subscribe, scheduler.now())
                    );
                    subscribers.remove(subscriber);
                }
            })));
        }
    }
}
