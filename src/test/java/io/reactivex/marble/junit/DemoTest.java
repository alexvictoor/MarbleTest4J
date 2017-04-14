package io.reactivex.marble.junit;


import io.reactivex.Observable;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Function;
import io.reactivex.marble.ColdObservable;
import org.junit.Rule;
import org.junit.Test;

import java.util.Map;

import static io.reactivex.marble.MapHelper.of;
import static io.reactivex.marble.junit.MarbleRule.*;

public class DemoTest {

    @Rule
    public MarbleRule marble = new MarbleRule();

    @Test
    public void should_map() {
        // given
        Observable<String> input = hot("a-b-c-d");
        // when
        Observable<String> output = input.map(new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s.toUpperCase();
            }
        });
        // then
        expectObservable(output).toBe("A-B-C-D");
    }

    @Test
    public void should_sum() {
        // given
        Observable<Integer> input = cold("a-b-c-d", of("a", 1, "b", 2, "c", 3, "d", 4));
        // when
        Observable<Integer> output = input.scan(new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer first, Integer second) {
                return first + second;
            }
        });
        // then
        expectObservable(output).toBe("A-B-C-D", of("A", 1, "B", 3, "C", 6, "D", 10));
    }

    @Test
    public void should_be_awesome() {
        Map<String, ?> values = of("a", 1, "b", 2);
        ColdObservable<?> myObservable
                = cold(                     "---a---b--|", values);
        String subs =                       "^---------!";
        expectObservable(myObservable).toBe("---a---b--|", values);
        expectSubscriptions(myObservable.getSubscriptions()).toBe(subs);
    }

    @Test
    public void should_use_unsubscription_diagram() {
        Observable<String> source = hot("---^-a-b-|");
        String unsubscribe =               "---!";
        String expected =                  "--a";
        expectObservable(source, unsubscribe).toBe(expected);
    }

}
