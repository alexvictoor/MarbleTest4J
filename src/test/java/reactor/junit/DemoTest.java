package reactor.junit;

import org.junit.Rule;
import org.junit.Test;
import reactor.ColdFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import static reactor.MapHelper.*;
import static reactor.junit.MarbleRule.expectFlux;
import static reactor.junit.MarbleRule.*;

/**
 * Created by Alexandre Victoor on 26/04/2017.
 */
public class DemoTest {

    @Rule
    public MarbleRule marble = new MarbleRule();

    @Test
    public void should_map() {
        // given
        Flux<String> input = hot("a-b-c-d");
        // when
        Flux<String> output = input.map(new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s.toUpperCase();
            }
        });
        // then
        expectFlux(output).toBe("A-B-C-D");
    }

    @Test
    public void should_sum() {
        // given
        Flux<Integer> input = cold("a-b-c-d", of("a", 1, "b", 2, "c", 3, "d", 4));
        // when
        Flux<Integer> output = input.scan(new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer first, Integer second) {
                return first + second;
            }
        });
        // then
        expectFlux(output).toBe("A-B-C-D", of("A", 1, "B", 3, "C", 6, "D", 10));
    }

    @Test
    public void should_be_awesome() {
        Map<String, ?> values = of("a", 1, "b", 2);
        ColdFlux<?> myFlux
                = cold(         "---a---b--|", values);
        String subs =           "^---------!";
        expectFlux(myFlux).toBe("---a---b--|", values);
        expectSubscriptions(myFlux.getSubscriptions()).toBe(subs);
    }

    @Test
    public void should_use_unsubscription_diagram() {
        Flux<String> source = hot("---^-a-b-|");
        String unsubscribe =         "---!";
        String expected =            "--a";
        expectFlux(source, unsubscribe).toBe(expected);
    }

    @Test
    public void should_map_mono() {
        // given
        Mono<String> input = hot("--(a|)").next();
        // when
        Mono<String> output = input.map(new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s.toUpperCase();
            }
        });
        // then
        expectMono(output).toBe("--(A|)");
    }
}
