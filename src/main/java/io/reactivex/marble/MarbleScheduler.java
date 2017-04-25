package io.reactivex.marble;


import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.annotations.NonNull;
import io.reactivex.schedulers.TestScheduler;
import org.reactivestreams.*;
import org.reactivestreams.ExpectSubscriptionsException;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


public class MarbleScheduler extends Scheduler {

    private final TestScheduler testScheduler = new TestScheduler();

    private final MarbleSchedulerState state;
    private final long frameTimeFactor;

    public MarbleScheduler() {
        this(10);
    }

    public MarbleScheduler(long frameTimeFactor) {
        this.frameTimeFactor = frameTimeFactor;
        state = new PatchedSchedulerState(frameTimeFactor, new MarbleSchedulerState.ISchedule() {
            @Override
            public long now() {
                return MarbleScheduler.this.now(TimeUnit.MILLISECONDS);
            }

            @Override
            public void schedule(Runnable runnable, long time) {
                MarbleScheduler.this.createWorker().schedule(runnable, time, TimeUnit.MILLISECONDS);
            }
        }, getClass());
    }

    @Override
    public long now(@NonNull TimeUnit unit) {
        return testScheduler.now(unit);
    }

    public void advanceTimeBy(long delayTime, TimeUnit unit) {
        testScheduler.advanceTimeBy(delayTime, unit);
    }

    public void advanceTimeTo(long delayTime, TimeUnit unit) {
        testScheduler.advanceTimeTo(delayTime, unit);
    }

    public void triggerActions() {
        testScheduler.triggerActions();
    }

    @Override
    @NonNull
    public Worker createWorker() {
        return testScheduler.createWorker();
    }

    public <T> ColdObservable<T> createColdObservable(String marbles, Map<String, T> values) {
        List<Recorded<T>> notifications = Parser.parseMarbles(marbles, values, null, frameTimeFactor);
        return ColdObservable.create(this, notifications);
    }

    public <T> ColdObservable<T> createColdObservable(String marbles) {
        return createColdObservable(marbles, null);
    }

    public <T> HotObservable<T> createHotObservable(String marbles, Map<String, T> values) {
        List<Recorded<T>> notifications = Parser.parseMarbles(marbles, values, null, frameTimeFactor);
        return HotObservable.create(this, notifications);
    }

    public <T> HotObservable<T> createHotObservable(String marbles) {
        return createHotObservable(marbles, null);
    }


    public long createTime(String marbles) {
        int endIndex = marbles.indexOf("|");
        if (endIndex == -1) {
            throw new RuntimeException("Marble diagram for time should have a completion marker '|'");
        }

        return endIndex * frameTimeFactor;
    }

    public void flush() {
        testScheduler.advanceTimeTo(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        try {
            state.flush();
        } catch (ExpectPublisherException ex) {
            throw new ExpectObservableException(ex.getMessage());
        } catch (ExpectSubscriptionsException ex) {
            throw new io.reactivex.marble.ExpectSubscriptionsException(ex.getMessage());
        }
    }

    public <T> ISetupTest expectObservable(Observable<T> observable) {
        return expectObservable(observable, null);
    }

    public <T> ISetupTest expectObservable(Observable<T> observable, String unsubscriptionMarbles) {
        return state.expectPublisher(observable.toFlowable(BackpressureStrategy.BUFFER), unsubscriptionMarbles);
    }

    public ISetupSubscriptionsTest expectSubscriptions(List<SubscriptionLog> subscriptions) {
        return state.expectSubscriptions(subscriptions);
    }

    public static class PatchedSchedulerState extends MarbleSchedulerState {

        public PatchedSchedulerState(long frameTimeFactor, ISchedule scheduler, Class schedulerClass) {
            super(frameTimeFactor, scheduler, schedulerClass);
        }

        @Override
        protected Object materializeInnerStreamWhenNeeded(Object value) {
            if (value instanceof Observable) {
                Flowable flowable = ((Observable) value).toFlowable(BackpressureStrategy.BUFFER);
                return materializeInnerPublisher(flowable, scheduler);
            }
            return super.materializeInnerStreamWhenNeeded(value);
        }
    }
}
