package io.reactivex.marble.junit;


import io.reactivex.*;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.marble.ColdObservable;
import org.junit.Rule;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.TimeUnit;

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

    @Test
    public void should_map_flowable() {
        // given
        Observable<String> input = hot("a-b-c-d");
        // when
        Flowable<String> output = input.toFlowable(BackpressureStrategy.BUFFER).map(new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s.toUpperCase();
            }
        });
        // then
        expectFlowable(output).toBe("A-B-C-D");
    }

    @Test
    public void should_use_unsubscription_diagram_with_flowable() {
        Flowable<String> source = hot("---^-a-b-|").toFlowable(BackpressureStrategy.DROP);
        String unsubscribe =               "---!";
        String expected =                  "--a";
        expectFlowable(source, unsubscribe).toBe(expected);
    }

    @Test
    public void should_map_single() {
        // given
        Observable<String> input = hot("--(a|)");
        // when
        Single<String> output = input.single("error").map(new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s.toUpperCase();
            }
        });
        // then
        expectSingle(output).toBe("--(A|)");
    }

    @Test
    public void should_map_maybe() {
        // given
        Observable<String> input = hot("--(a|)");
        // when
        Maybe<String> output = input.singleElement().map(new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s.toUpperCase();
            }
        });
        // then
        expectMaybe(output).toBe("--(A|)");
    }

    @Test
    public void should_delay_completable() {
        // given
        Observable<String> input = hot("--|");
        // when
        Completable output = input.ignoreElements().delay(10, TimeUnit.MILLISECONDS, marble.scheduler);

        // then
        expectCompletable(output).toBe("---|");
    }

}
