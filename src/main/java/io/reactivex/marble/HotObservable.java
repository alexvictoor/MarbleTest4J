package io.reactivex.marble;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.Scheduler;
import org.reactivestreams.*;

import java.util.Arrays;
import java.util.List;


public class HotObservable<T> extends Observable<T> implements TestablePublisher<T> {

    private final TestablePublisher<T> publisher;

    protected HotObservable(TestablePublisher<T> publisher) {
        this.publisher = publisher;
    }

    @Override
    public void subscribe(Subscriber<? super T> s) {
        publisher.subscribe(s);
    }

    @Override
    protected void subscribeActual(final Observer<? super T> observer) {
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

    public static <T> HotObservable<T> create(Scheduler scheduler, Recorded<T>... notifications) {
        return create(scheduler, Arrays.asList(notifications));
    }

    public static <T> HotObservable<T> create(Scheduler scheduler, List<Recorded<T>> notifications) {
        HotPublisher<T> hotPublisher = new HotPublisher<>(new SchedulerAdapter(scheduler), notifications);
        return new HotObservable<>(hotPublisher);
    }

}
