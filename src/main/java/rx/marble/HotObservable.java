package rx.marble;

import rx.Notification;
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


public class HotObservable<T> extends Observable<T> implements TestableObservable<T> {

    private final List<Recorded<T>> notifications;
    List<SubscriptionLog> subscriptions = new ArrayList<>();

    protected HotObservable(OnSubscribe<T> f, List<Recorded<T>> notifications) {
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

    public static <T> HotObservable<T> create(Scheduler scheduler, Recorded<T>... notifications) {
        return create(scheduler, Arrays.asList(notifications));
    }

    public static <T> HotObservable<T> create(Scheduler scheduler, List<Recorded<T>> notifications) {
        OnSubscribeHandler<T> onSubscribeFunc = new OnSubscribeHandler<>(scheduler, notifications);
        HotObservable<T> observable = new HotObservable<>(onSubscribeFunc, notifications);
        onSubscribeFunc.observable = observable;
        return observable;
    }

    private static class OnSubscribeHandler<T> implements Observable.OnSubscribe<T> {

        private final Scheduler scheduler;
        private final List<Subscriber<? super T>> subscribers = new ArrayList<>();
        public HotObservable<T> observable;

        public OnSubscribeHandler(Scheduler scheduler, List<Recorded<T>> notifications) {
            this.scheduler = scheduler;
            Scheduler.Worker worker = scheduler.createWorker();
            for (final Recorded<T> event : notifications) {
                worker.schedule(new Action0() {
                    @Override
                    public void call() {
                        List<Subscriber<? super T>> subscribers
                                = new ArrayList<>(OnSubscribeHandler.this.subscribers);
                        for (Subscriber<? super T> subscriber : subscribers){
                            event.value.accept(subscriber);
                        }
                    }
                }, event.time, TimeUnit.MILLISECONDS);
            }
        }

        public void call(final Subscriber<? super T> subscriber) {
            final SubscriptionLog subscriptionLog = new SubscriptionLog(scheduler.now());
            observable.subscriptions.add(subscriptionLog);
            final int subscriptionIndex = observable.getSubscriptions().size() - 1;

            subscribers.add(subscriber);

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
        }
    }
}
