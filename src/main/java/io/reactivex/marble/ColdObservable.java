package io.reactivex.marble;


import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.Scheduler;
import org.reactivestreams.*;

import java.util.Arrays;
import java.util.List;


public class ColdObservable<T> extends Observable<T> implements TestablePublisher<T> {

    private final TestablePublisher<T> publisher;

    protected ColdObservable(TestablePublisher<T> publisher) {
        this.publisher = publisher;
    }

    @Override
    public void subscribe(Subscriber<? super T> s) {
        publisher.subscribe(s);
    }

    @Override
    protected void subscribeActual(Observer<? super T> observer) {
        publisher.subscribe(new ObserverAdapter<>(observer));
    }

    @Override
    public List<SubscriptionLog> getSubscriptions() {
        return publisher.getSubscriptions();
    }

    @Override
    public List<Recorded<T>> getMessages() {
        return publisher.getMessages();
    }

    public static <T> ColdObservable<T> create(Scheduler scheduler, Recorded<T>... notifications) {
        return create(scheduler, Arrays.asList(notifications));
    }

    public static <T> ColdObservable<T> create(final Scheduler scheduler, List<Recorded<T>> notifications) {

        ColdPublisher<T> coldPublisher = new ColdPublisher<>(new SchedulerFactory() {
            @Override
            public org.reactivestreams.Scheduler create() {
                return new SchedulerAdapter(scheduler);
            }
        }, notifications);

        return new ColdObservable<>(coldPublisher);
    }

}
