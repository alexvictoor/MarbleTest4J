package io.reactivex.marble;

import io.reactivex.Scheduler;

import java.util.concurrent.TimeUnit;

/**
 * Created by Alexandre Victoor on 18/04/2017.
 */
class SchedulerAdapter implements org.reactivestreams.Scheduler {

    private final Scheduler scheduler;
    private final Scheduler.Worker worker;

    public SchedulerAdapter(Scheduler scheduler) {
        this.scheduler = scheduler;
        this.worker = scheduler.createWorker();
    }

    @Override
    public void schedule(Runnable run, long delay, TimeUnit unit) {
        worker.schedule(run, delay, unit);
    }

    @Override
    public long now(TimeUnit unit) {
        return scheduler.now(unit);
    }

    @Override
    public void dispose() {
        worker.dispose();
    }
}