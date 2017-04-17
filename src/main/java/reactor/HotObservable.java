package reactor;


import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class HotObservable<T> extends Flux<T> implements TestableObservable<T> {

    private final List<Recorded<T>> notifications;
    private final List<Subscriber<? super T>> observers = new ArrayList<>();
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
                for (Subscriber<? super T> observer : observers){
                    event.value.accept(observer);
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
    public void subscribe(final Subscriber<? super T> subscriber) {

        observers.add(subscriber);

        final SubscriptionLog subscriptionLog = new SubscriptionLog(scheduler.now(TimeUnit.MILLISECONDS));
        subscriptions.add(subscriptionLog);
        final int subscriptionIndex = subscriptions.size() - 1;

        subscriber.onSubscribe(new Subscription() {
            @Override
            public void request(long l) {
                // TODO check if useful
            }

            @Override
            public void cancel() {
                observers.remove(subscriber);
                subscriptions.set(
                    subscriptionIndex,
                    new SubscriptionLog(subscriptionLog.subscribe, scheduler.now(TimeUnit.MILLISECONDS))
                );
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
