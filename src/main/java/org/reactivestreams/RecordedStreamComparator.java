package org.reactivestreams;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Alexandre Victoor on 25/10/2016.
 */
public class RecordedStreamComparator {

    public StreamComparison compare(
            List<Recorded<?>> actualRecords,
            List<Recorded<?>> expectedRecords) {

        List<EventComparison> unitComparisons = new ArrayList<>();

        List<Recorded<?>> onlyOnExpected = new ArrayList<>(expectedRecords);
        onlyOnExpected.removeAll(actualRecords);
        for (Recorded<?> record : onlyOnExpected) {
            unitComparisons.add(new EventComparison(record, EventComparisonResult.ONLY_ON_EXPECTED));
        }

        List<Recorded<?>> onlyOnActual = new ArrayList<>(actualRecords);
        onlyOnActual.removeAll(expectedRecords);
        for (Recorded<?> record : onlyOnActual) {
            unitComparisons.add(new EventComparison(record, EventComparisonResult.ONLY_ON_ACTUAL));
        }

        boolean equalStreams = unitComparisons.isEmpty();

        for (Recorded<?> record : actualRecords){
            if (!onlyOnActual.contains(record) && !onlyOnExpected.contains(record)) {
                unitComparisons.add(new EventComparison(record, EventComparisonResult.EQUALS));
            }
        }
        Collections.sort(unitComparisons, new Comparator<EventComparison>() {
            @Override
            public int compare(EventComparison first, EventComparison second) {

                int diff = new Long(first.record.time - second.record.time).intValue();
                if (diff == 0) {
                    //
                    // if events are simultaneous
                    // on complete and on error should be last
                    //
                    if (first.record.value.isOnComplete() || first.record.value.isOnError()) {
                        return Integer.MAX_VALUE;
                    }
                    if (second.record.value.isOnComplete() || second.record.value.isOnError()) {
                        return Integer.MIN_VALUE;
                    }
                }
                return diff;
            }
        });

        return new StreamComparison(equalStreams, unitComparisons);
    }


    public static class StreamComparison {
        public final boolean streamEquals;
        public final List<EventComparison> unitComparisons;

        public StreamComparison(boolean equals, List<EventComparison> comparisons) {
            this.streamEquals = equals;
            this.unitComparisons = comparisons;
        }

        public String toString() {
            StringBuilder builder = new StringBuilder();
            if (streamEquals) {
                builder.append("Streams are equal");
            } else {
                builder.append("Streams are not equal, details below:");
                EventComparisonResult currentMode = null;
                for (EventComparison comparison: unitComparisons) {
                    if (currentMode == comparison.result) {
                        builder.append("\n");
                    } else {
                        currentMode = comparison.result;
                        switch (currentMode) {
                            case EQUALS:
                                builder.append("\n=   On actual & expected streams\n");
                                break;
                            case ONLY_ON_ACTUAL:
                                builder.append("\n+   On actual stream\n");
                                break;
                            case ONLY_ON_EXPECTED:
                                builder.append("\n-   On expected stream\n");
                                break;
                        }
                    }
                    builder.append(comparison);
                }
            }
            return builder.toString();
        }
    }

    public enum EventComparisonResult { EQUALS, ONLY_ON_ACTUAL, ONLY_ON_EXPECTED }

    public static class EventComparison {
        public final Recorded<?> record;
        public final EventComparisonResult result;

        public EventComparison(Recorded<?> record, EventComparisonResult result) {
            this.record = record;
            this.result = result;
        }

        @Override
        public String toString() {
            switch (result) {
                case EQUALS:
                    return "= " + record.toString().replace("\n", "\n= ");
                case ONLY_ON_ACTUAL:
                    return "+ " + record.toString().replace("\n", "\n+ ");
                case ONLY_ON_EXPECTED:
                    return "- " + record.toString().replace("\n", "\n- ");
                default: throw new RuntimeException("should not happen");
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            EventComparison that = (EventComparison) o;

            if (!record.equals(that.record)) return false;
            return result == that.result;

        }

        @Override
        public int hashCode() {
            int result1 = record.hashCode();
            result1 = 31 * result1 + result.hashCode();
            return result1;
        }
    }

}
