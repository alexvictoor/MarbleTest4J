package rx.marble;

import org.junit.Test;
import rx.Notification;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static rx.Notification.createOnError;
import static rx.Notification.createOnNext;
import static rx.marble.RecordedStreamComparator.EventComparisonResult.*;

/**
 * Created by Alexandre Victoor on 26/10/2016.
 */
public class RecordedStreamComparatorTest {


    @Test
    public void should_detect_missing_event_in_actual_records() {
        // given
        Recorded<?> onCompletedEvent = new Recorded<>(10, Notification.createOnCompleted());
        List<Recorded<?>> actualRecords = asList();
        List<Recorded<?>> expectedRecords = new ArrayList<>();
        expectedRecords.add(onCompletedEvent);

        // when
        RecordedStreamComparator.StreamComparison result
                = new RecordedStreamComparator().compare(actualRecords, expectedRecords);
        // then
        assertThat(result.streamEquals).isFalse();
        assertThat(result.unitComparisons).hasSize(1);
        assertThat(result.unitComparisons.get(0)).isEqualToComparingFieldByField(
                new RecordedStreamComparator.EventComparison(onCompletedEvent, RecordedStreamComparator.EventComparisonResult.ONLY_ON_EXPECTED)
        );
    }

    @Test
    public void should_detect_additional_event_in_actual_records() {
        // given
        Recorded<Object> onCompletedEvent = new Recorded<>(10, Notification.createOnCompleted());
        List<Recorded<?>> actualRecords = new ArrayList<>();
        actualRecords.add(onCompletedEvent);
        List<Recorded<?>> expectedRecords = asList();

        // when
        RecordedStreamComparator.StreamComparison result
                = new RecordedStreamComparator().compare(actualRecords, expectedRecords);
        // then
        assertThat(result.streamEquals).isFalse();
        assertThat(result.unitComparisons).hasSize(1);
        assertThat(result.unitComparisons.get(0)).isEqualToComparingFieldByField(
                new RecordedStreamComparator.EventComparison(onCompletedEvent, ONLY_ON_ACTUAL)
        );
    }

    @Test
    public void should_detect_identical_streams() {
        // given
        List<Recorded<?>> actualRecords = new ArrayList<>();
        actualRecords.add(new Recorded<>(10, createOnNext(12)));
        List<Recorded<?>> expectedRecords = new ArrayList<>();
        expectedRecords.add(new Recorded<>(10, createOnNext(12)));
        // when
        RecordedStreamComparator.StreamComparison result
                = new RecordedStreamComparator().compare(actualRecords, expectedRecords);
        // then
        assertThat(result.streamEquals).isTrue();
    }

    @Test
    public void should_detect_identical_streams_ending_on_error() {
        // given
        List<Recorded<?>> actualRecords = new ArrayList<>();
        actualRecords.add(new Recorded<>(10, createOnError(new Exception("whatever"))));
        List<Recorded<?>> expectedRecords = new ArrayList<>();
        expectedRecords.add(new Recorded<>(10, createOnError(new Exception("whatever"))));
        // when
        RecordedStreamComparator.StreamComparison result
                = new RecordedStreamComparator().compare(actualRecords, expectedRecords);
        // then
        assertThat(result.streamEquals).isTrue();
    }

    @Test
    public void should_detect_equal_and_different_records() {
        // given
        Recorded<?> onCompletedEvent = new Recorded<>(20, Notification.createOnCompleted());
        List<Recorded<?>> actualRecords = asList(
                new Recorded<>(5, createOnNext(12)),
                onCompletedEvent
        );
        List<Recorded<?>> expectedRecords = asList(
                new Recorded<>(15, createOnNext(36)),
                onCompletedEvent
        );
        // when
        RecordedStreamComparator.StreamComparison result
                = new RecordedStreamComparator().compare(actualRecords, expectedRecords);
        // then
        assertThat(result.streamEquals).isFalse();
        assertThat(result.unitComparisons).hasSize(3);
        assertThat(result.unitComparisons).containsExactly(
                new RecordedStreamComparator.EventComparison(new Recorded<>(5, createOnNext((Object)12)), ONLY_ON_ACTUAL),
                new RecordedStreamComparator.EventComparison(new Recorded<>(15, createOnNext((Object)36)), ONLY_ON_EXPECTED),
                new RecordedStreamComparator.EventComparison(onCompletedEvent, EQUALS)
        );

    }

    @Test
    public void should_put_on_completed_events_after() {
        // given
        Recorded<?> onCompletedEvent = new Recorded<>(20, Notification.createOnCompleted());
        List<Recorded<?>> actualRecords = asList(
                new Recorded<>(20, createOnNext(12)),
                onCompletedEvent
        );
        List<Recorded<?>> expectedRecords = new ArrayList<>();
        expectedRecords.add(new Recorded<>(20, createOnNext(12)));
        // when
        RecordedStreamComparator.StreamComparison result
                = new RecordedStreamComparator().compare(actualRecords, expectedRecords);
        // then
        assertThat(result.streamEquals).isFalse();
        assertThat(result.unitComparisons).hasSize(2);
        assertThat(result.unitComparisons).containsExactly(
                new RecordedStreamComparator.EventComparison(new Recorded<>(20, createOnNext(12)), EQUALS),
                new RecordedStreamComparator.EventComparison(onCompletedEvent, ONLY_ON_ACTUAL)
        );

    }


}