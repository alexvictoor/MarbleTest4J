package rx.marble;

/**
 * Created by Alexandre Victoor on 05/06/2016.
 */
public class Recorded<T> {

    public final T value;
    public final long time;

    public Recorded(long time, T value) {
        this.time = time;
        this.value = value;
    }


    @Override
    public String toString() {
        return "Recorded{" +
                "value=" + value +
                ", time=" + time +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Recorded<?> recorded = (Recorded<?>) o;

        if (time != recorded.time) return false;
        return !(value != null ? !value.equals(recorded.value) : recorded.value != null);

    }

    @Override
    public int hashCode() {
        int result = value != null ? value.hashCode() : 0;
        result = 31 * result + (int) (time ^ (time >>> 32));
        return result;
    }
}
