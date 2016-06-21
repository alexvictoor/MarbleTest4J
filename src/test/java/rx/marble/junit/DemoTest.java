package rx.marble.junit;


import org.junit.Rule;
import org.junit.Test;
import rx.Observable;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.marble.ColdObservable;

import java.util.Map;

import static rx.marble.MapHelper.of;
import static rx.marble.junit.MarbleRule.*;

public class DemoTest {

    @Rule
    public MarbleRule marble = new MarbleRule();

    @Test
    public void should_map() {
        // given
        Observable<String> input = hot("a-b-c-d");
        // when
        Observable<String> output = input.map(new Func1<String, String>() {
            @Override
            public String call(String s) {
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
        Observable<Integer> output = input.scan(new Func2<Integer, Integer, Integer>() {
            @Override
            public Integer call(Integer first, Integer second) {
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
