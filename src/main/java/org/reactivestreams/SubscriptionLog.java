package org.reactivestreams;

/**
 * Created by Alexandre Victoor on 08/06/2016.
 */
public class SubscriptionLog {
    public final long subscribe;
    public final long unsubscribe;

    public SubscriptionLog(long subscribe) {
        this.subscribe = subscribe;
        this.unsubscribe = Long.MAX_VALUE;
    }

    public SubscriptionLog(long subscribe, long unsubscribe) {
        this.subscribe = subscribe;
        this.unsubscribe = unsubscribe;
    }

    public boolean doesNeverEnd() {
        return unsubscribe == Long.MAX_VALUE;
    }

    @Override
    public String toString() {
        return "SubscriptionLog{" +
                "subscribe=" + subscribe +
                ", unsubscribe=" + unsubscribe +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SubscriptionLog that = (SubscriptionLog) o;

        if (subscribe != that.subscribe) return false;
        return unsubscribe == that.unsubscribe;

    }

    @Override
    public int hashCode() {
        int result = (int) (subscribe ^ (subscribe >>> 32));
        result = 31 * result + (int) (unsubscribe ^ (unsubscribe >>> 32));
        return result;
    }
}
