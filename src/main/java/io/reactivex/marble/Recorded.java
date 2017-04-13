package io.reactivex.marble;

import io.reactivex.Notification;

/**
 * Created by Alexandre Victoor on 05/06/2016.
 */
public class Recorded<T> {

    public final Notification<T> value;
    public final long time;

    public Recorded(long time, Notification<T> value) {
        this.time = time;
        this.value = value;
    }


    @Override
    public String toString() {

        final String valueString;

        if (value.isOnComplete()) {
            valueString = "On Completed";
        } else if (value.isOnError()) {
            valueString =  "On Error";
        } else {
            valueString = "On Next: " + value.getValue();
        }

        return "{\n" +
                "  time = " + time +
                "\n  " + valueString +
                "\n}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Recorded<?> recorded = (Recorded<?>) o;

        if (time != recorded.time) return false;

        return !(value != null ? ! notificationsAreEqual(value, recorded.value) : recorded.value != null);
    }

    private boolean notificationsAreEqual(Notification<?> first, Notification<?> second) {
        if (first == null || second == null) {
            return false;
        }

        if (first.isOnError() && second.isOnError()) {
           // we do not do deep comparisons on exceptions
            return true;
        }
        return first.equals(second);
    }

    @Override
    public int hashCode() {
        int result = value != null ? value.hashCode() : 0;
        result = 31 * result + (int) (time ^ (time >>> 32));
        return result;
    }
}
