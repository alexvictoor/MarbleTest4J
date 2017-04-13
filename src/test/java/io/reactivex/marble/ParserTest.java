package io.reactivex.marble;


import org.junit.Test;
import rx.Notification;
import rx.marble.ColdObservable;
import rx.marble.Parser;
import rx.marble.Recorded;
import rx.marble.SubscriptionLog;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;


public class ParserTest {

    @Test
    public void should_parse_a_marble_string_into_a_series_of_notifications_and_types() {
        Map<String, Object> events = new HashMap<>();
        events.put("a", "A");
        events.put("b", "B");
        List<Recorded<Object>> result = Parser.parseMarbles("-------a---b---|", events, 10);

        assertThat(result).containsExactly(
                new Recorded<>(70, Notification.createOnNext((Object)"A")),
                new Recorded<>(110, Notification.createOnNext((Object)"B")),
                new Recorded<>(150, Notification.createOnCompleted())
        );
    }

    @Test
    public void should_parse_a_marble_string_allowing_spaces_too() {
        Map<String, Object> events = new HashMap<>();
        events.put("a", "A");
        events.put("b", "B");
        List<Recorded<Object>> result = Parser.parseMarbles("--a--b--|   ", events, 10);

        assertThat(result).containsExactly(
                new Recorded<>(20, Notification.createOnNext((Object)"A")),
                new Recorded<>(50, Notification.createOnNext((Object)"B")),
                new Recorded<>(80, Notification.createOnCompleted())
        );
    }

    @Test
    public void should_parse_a_marble_string_with_a_subscription_point() {
        Map<String, Object> events = new HashMap<>();
        events.put("a", "A");
        events.put("b", "B");
        List<Recorded<Object>> result = Parser.parseMarbles("---^---a---b---|", events, 10);

        assertThat(result).containsExactly(
                new Recorded<>(40, Notification.createOnNext((Object)"A")),
                new Recorded<>(80, Notification.createOnNext((Object)"B")),
                new Recorded<>(120, Notification.createOnCompleted())
        );
    }

    @Test
    public void should_parse_a_marble_string_with_an_error() {
        Exception errorValue = new Exception("omg error!");
        Map<String, Object> events = new HashMap<>();
        events.put("a", "A");
        events.put("b", "B");
        List<Recorded<Object>> result = Parser.parseMarbles("-------a---b---#", events, errorValue, 10);

        assertThat(result).containsExactly(
                new Recorded<>(70, Notification.createOnNext((Object)"A")),
                new Recorded<>(110, Notification.createOnNext((Object)"B")),
                new Recorded<>(150, Notification.createOnError(errorValue))
        );
    }

    @Test
    public void should_default_in_the_letter_for_the_value_if_no_value_hash_was_passed() {
        List<Recorded<String>> result = Parser.parseMarbles("--a--b--|", 10);

        assertThat(result).containsExactly(
                new Recorded<>(20, Notification.createOnNext("a")),
                new Recorded<>(50, Notification.createOnNext("b")),
                new Recorded<>(80, Notification.<String>createOnCompleted())
        );
    }

    @Test
    public void should_handle_grouped_values() {
        List<Recorded<String>> result = Parser.parseMarbles("---(abc)---", 10);

        assertThat(result).containsExactly(
                new Recorded<>(30, Notification.createOnNext("a")),
                new Recorded<>(30, Notification.createOnNext("b")),
                new Recorded<>(30, Notification.createOnNext("c"))
        );
    }

    @Test
    public void should_handle_grouped_values_at_zero_time() {
        List<Recorded<String>> result = Parser.parseMarbles("(abc)---", 10);

        assertThat(result).containsExactly(
                new Recorded<>(0, Notification.createOnNext("a")),
                new Recorded<>(0, Notification.createOnNext("b")),
                new Recorded<>(0, Notification.createOnNext("c"))
        );
    }

    @Test
    public void should_handle_value_after_grouped_values() {
        List<Recorded<String>> result = Parser.parseMarbles("---(abc)d--", 10);

        assertThat(result).containsExactly(
                new Recorded<>(30, Notification.createOnNext("a")),
                new Recorded<>(30, Notification.createOnNext("b")),
                new Recorded<>(30, Notification.createOnNext("c")),
                new Recorded<>(80, Notification.createOnNext("d"))
        );
    }

    @Test
    public void should_parse_a_subscription_marble_string_into_a_subscriptionLog() {
        SubscriptionLog result = Parser.parseMarblesAsSubscriptions("---^---!-", 10);

        assertThat(result.subscribe).isEqualTo(30);
        assertThat(result.unsubscribe).isEqualTo(70);
    }

    @Test
    public void should_parse_a_subscription_marble_without_an_unsubscription() {
        SubscriptionLog result = Parser.parseMarblesAsSubscriptions("---^---", 10);

        assertThat(result.subscribe).isEqualTo(30);
        assertThat(result.unsubscribe).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    public void should_parse_a_subscription_marble_with_a_synchronous_unsubscription() {
        SubscriptionLog result = Parser.parseMarblesAsSubscriptions("---(^!)---", 10);

        assertThat(result.subscribe).isEqualTo(30);
        assertThat(result.unsubscribe).isEqualTo(30);
    }

    @Test
    public void should_parse_a_marble_string_with_observable_values() {

        ColdObservable<Integer> aObservable
                = ColdObservable.create(null, new Recorded<>(20, Notification.createOnNext(123)));
        Map<String, Object> events = new HashMap<>();
        events.put("a", aObservable);
        List<Recorded<Object>> result = Parser.parseMarbles("-a-", events, null, 10, true);

        assertThat(result).containsExactly(
                new Recorded<>(10,
                        Notification.createOnNext(
                                (Object)Arrays.asList(
                                        new Recorded<>(20, Notification.createOnNext(123))
                                )
                        )
                )
        );
    }
}