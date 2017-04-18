package org.reactivestreams;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by Alexandre Victoor on 18/04/2017.
 */
public class ColdPublisher<T> implements TestablePublisher<T> {

    private final SchedulerFactory schedulerFactory;
    private final List<Recorded<T>> recordedNotifications;
    private final List<SubscriptionLog> subscriptions = new ArrayList<>();


    public ColdPublisher(SchedulerFactory schedulerFactory, List<Recorded<T>> notifications) {
        this.schedulerFactory = schedulerFactory;
        this.recordedNotifications = notifications;
    }

    @Override
    public void subscribe(final Subscriber<? super T> observer) {
        final Scheduler scheduler = schedulerFactory.create();
        final SubscriptionLog subscriptionLog = new SubscriptionLog(scheduler.now(TimeUnit.MILLISECONDS));
        subscriptions.add(subscriptionLog);
        final int subscriptionIndex = subscriptions.size() - 1;
        for (final Recorded<T> event: recordedNotifications) {
            scheduler.schedule(new Runnable() {
                @Override
                public void run() {
                    event.value.accept(observer);
                    if (!event.value.isOnNext()) {
                        endSubscriptions(event.time);
                    }
                }
            }, event.time, TimeUnit.MILLISECONDS);
        }

        observer.onSubscribe(new Subscription() {

            private boolean disposed = false;

            @Override
            public void request(long n) {
                // TODO
            }

            @Override
            public void cancel() {
                disposed = true;
                subscriptions.set(
                        subscriptionIndex,
                        new SubscriptionLog(subscriptionLog.subscribe, scheduler.now(TimeUnit.MILLISECONDS))
                );
                scheduler.dispose();
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

}
