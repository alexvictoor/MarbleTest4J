package rx.marble;


import org.junit.Test;
import rx.Notification;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by Alexandre Victoor on 05/06/2016.
 */
public class ParserTest {

    @Test
    public void should_parse_a_marble_string_into_a_series_of_notifications_and_types()
    {
        Map<String, Object> events = new HashMap<>();
        events.put("a", "A");
        events.put("b", "B");
        List<Recorded<Notification<Object>>> result = Parser.parseMarbles("-------a---b---|", events);

        assertThat(result).containsExactly(
                new Recorded<>(70, Notification.createOnNext((Object)"A")),
                new Recorded<>(110, Notification.createOnNext((Object)"B")),
                new Recorded<>(150, Notification.createOnCompleted())
        );
    }

    @Test
    public void should_parse_a_marble_string_allowing_spaces_too()
    {
        Map<String, Object> events = new HashMap<>();
        events.put("a", "A");
        events.put("b", "B");
        List<Recorded<Notification<Object>>> result = Parser.parseMarbles("--a--b--|   ", events);

        assertThat(result).containsExactly(
                new Recorded<>(20, Notification.createOnNext((Object)"A")),
                new Recorded<>(50, Notification.createOnNext((Object)"B")),
                new Recorded<>(80, Notification.createOnCompleted())
        );
    }

    @Test
    public void should_parse_a_marble_string_with_a_subscription_point()
    {
        Map<String, Object> events = new HashMap<>();
        events.put("a", "A");
        events.put("b", "B");
        List<Recorded<Notification<Object>>> result = Parser.parseMarbles("---^---a---b---|", events);

        assertThat(result).containsExactly(
                new Recorded<>(40, Notification.createOnNext((Object)"A")),
                new Recorded<>(80, Notification.createOnNext((Object)"B")),
                new Recorded<>(120, Notification.createOnCompleted())
        );
    }

    @Test
    public void should_parse_a_marble_string_with_an_error()
    {
        Exception errorValue = new Exception("omg error!");
        Map<String, Object> events = new HashMap<>();
        events.put("a", "A");
        events.put("b", "B");
        List<Recorded<Notification<Object>>> result = Parser.parseMarbles("-------a---b---#", events, errorValue);

        assertThat(result).containsExactly(
                new Recorded<>(70, Notification.createOnNext((Object)"A")),
                new Recorded<>(110, Notification.createOnNext((Object)"B")),
                new Recorded<>(150, Notification.createOnError(errorValue))
        );
    }

    @Test
    public void should_default_in_the_letter_for_the_value_if_no_value_hash_was_passed()
    {
        List<Recorded<Notification<String>>> result = Parser.parseMarbles("--a--b--|");

        assertThat(result).containsExactly(
                new Recorded<>(20, Notification.createOnNext("a")),
                new Recorded<>(50, Notification.createOnNext("b")),
                new Recorded<>(80, Notification.<String>createOnCompleted())
        );
    }

    @Test
    public void should_handle_grouped_values()
    {
        List<Recorded<Notification<String>>> result = Parser.parseMarbles("---(abc)---");

        assertThat(result).containsExactly(
                new Recorded<>(30, Notification.createOnNext("a")),
                new Recorded<>(30, Notification.createOnNext("b")),
                new Recorded<>(30, Notification.createOnNext("c"))
        );
    }

    @Test
    public void should_handle_grouped_values_at_zero_time()
    {
        List<Recorded<Notification<String>>> result = Parser.parseMarbles("(abc)---");

        assertThat(result).containsExactly(
                new Recorded<>(0, Notification.createOnNext("a")),
                new Recorded<>(0, Notification.createOnNext("b")),
                new Recorded<>(0, Notification.createOnNext("c"))
        );
    }

    @Test
    public void should_handle_value_after_grouped_values()
    {
        List<Recorded<Notification<String>>> result = Parser.parseMarbles("---(abc)d--");

        assertThat(result).containsExactly(
                new Recorded<>(30, Notification.createOnNext("a")),
                new Recorded<>(30, Notification.createOnNext("b")),
                new Recorded<>(30, Notification.createOnNext("c")),
                new Recorded<>(80, Notification.createOnNext("d"))
        );
    }



}