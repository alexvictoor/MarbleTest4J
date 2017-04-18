package org.reactivestreams;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by Alexandre Victoor on 18/04/2017.
 */
public class HotPublisher<T> implements Publisher<T>, TestablePublisher<T> {

    private final List<Recorded<T>> notifications;
    private final List<Subscriber<? super T>> observers = new ArrayList<>();
    private final Scheduler scheduler;
    List<SubscriptionLog> subscriptions = new ArrayList<>();

    public HotPublisher(Scheduler scheduler, List<Recorded<T>> notifications) {
        this.scheduler = scheduler;
        this.notifications = notifications;
        scheduleNotifications();
    }

    private void scheduleNotifications() {
        for (final Recorded<T> event : notifications) {
            scheduler.schedule(new Runnable() {
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

}
