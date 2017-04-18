package reactor;

import org.reactivestreams.*;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Alexandre Victoor on 18/04/2017.
 */
public class ColdFlux<T> extends Flux<T> implements TestablePublisher<T> {

    private final TestablePublisher<T> publisher;

    protected ColdFlux(TestablePublisher<T> publisher) {
        this.publisher = publisher;
    }

    @Override
    public void subscribe(Subscriber<? super T> s) {
        publisher.subscribe(s);
    }

    @Override
    public List<SubscriptionLog> getSubscriptions() {
        return publisher.getSubscriptions();
    }

    @Override
    public List<Recorded<T>> getMessages() {
        return publisher.getMessages();
    }

    public static <T> ColdFlux<T> create(Scheduler scheduler, Recorded<T>... notifications) {
        return create(scheduler, Arrays.asList(notifications));
    }

    public static <T> ColdFlux<T> create(final Scheduler scheduler, List<Recorded<T>> notifications) {

        ColdPublisher<T> coldPublisher = new ColdPublisher<>(new SchedulerFactory() {
            @Override
            public org.reactivestreams.Scheduler create() {
                return new SchedulerAdapter(scheduler);
            }
        }, notifications);

        return new ColdFlux<>(coldPublisher);
    }

}
