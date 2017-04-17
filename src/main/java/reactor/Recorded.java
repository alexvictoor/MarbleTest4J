package reactor;

import reactor.core.publisher.Signal;

/**
 * Created by Alexandre Victoor on 05/06/2016.
 */
public class Recorded<T> {

    public final Signal<T> value;
    public final long time;

    public Recorded(long time, Signal<T> value) {
        this.time = time;
        this.value = value;
    }

    @Override
    public String toString() {

        return "{\n" +
                "  time = " + time +
                "\n  " + value +
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

    private boolean notificationsAreEqual(Signal<?> first, Signal<?> second) {
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
