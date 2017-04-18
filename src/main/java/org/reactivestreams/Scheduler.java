package org.reactivestreams;

import java.util.concurrent.TimeUnit;

/**
 * Created by Alexandre Victoor on 18/04/2017.
 */
public interface Scheduler {

    void schedule(Runnable run, long delay, TimeUnit unit);

    long now(TimeUnit unit);

    void dispose();
}
