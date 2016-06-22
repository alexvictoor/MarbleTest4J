[![Build Status](https://travis-ci.org/alexvictoor/MarbleTest4J.svg?branch=master)](https://travis-ci.org/alexvictoor/MarbleTest4J)

# MarbleTest4j
Java port of RxJS marble tests

MarbleTest4j is a tiny library that allows to write tests for codes based on RxJava using marble diagrams in ASCII form.  
This is a Java port of the [marble test features](https://github.com/ReactiveX/rxjs/blob/master/doc/writing-marble-tests.md) of amazing RxJS v5.  
The purpose of the library is to help you write as concise an readable tests when dealing with Rx code, 
bringing a developer experience as close as possible as the one of RxJS. 

## Quickstart

To get the lib just use add a maven dependency as below:
```xml
<dependency>
  <groupId>com.github.alexvictoor</groupId>
  <artifactId>marbletest4j</artifactId>
  <version>1.0</version>
</dependency>
```

## Usage (the concise way)
A jUnit integration is provided in order to let you write concise tests as you would have done with RxJS.
This integration is made of a jUnit rule **MarbleRule** and a bunch of static methods providing aliases to MarbleScheduler's methods. 
**MarbleScheduler** is very similar to RxJS TestScheduler: it is like RxJava's TestScheduler plus marble related methods to create hot & cold 
test observables and then perform assertions. Obviously everything is done using marble schemas in ASCII form.     
**MarbleRule** keeps in a threadlocal reference a **MarbleScheduler** instance that will be used by static aliases methods. 
Though, for most cases you will not need to manipulate directly any scheduler.   
Below a complete example:
```
import static rx.marble.junit.MarbleRule.*;

@Rule
public MarbleRule marble = new MarbleRule();

@Test
public void should_map() {
    // given
    Observable<String> input = hot("a-b-c-d");
    // when
    Observable<String> output = input.map((s) -> s.toUpperCase());
    // then
    expectObservable(output).toBe("A-B-C-D");
}
```
In the example above, we create first a hot observable trigering events 'a', 'b', 'c', 'd' (at 0, 20, 40 and 60)  
Then we perform some transformations, using rx map operator, and last we perform an assertion on generated Observable.  
In previous example event values were strings, other types are also supported:
```
import static rx.marble.junit.MarbleRule.*;
import static rx.marble.MapHelper.of;

@Rule
public MarbleRule marble = new MarbleRule();

@Test
public void should_subscribe_during_the_test() {
    Map<String, Integer> values = of("a", 1, "b", 2); // shortcut to create a Map
    
    ColdObservable<Integer> myObservable
                = cold(                 "---a---b--|", values);
    String subs =                       "^---------!";
    
    expectObservable(myObservable).toBe("---a---b--|", values);
    expectSubscriptions(myObservable.getSubscriptions()).toBe(subs);
}
```
As shown above, you can check events timing and values, but also when subscriptions start and end.  
Everything in a visual way using marble diagrams in ASCII forms :-)

## Usage (the verbose way)

As said before, the API sticks to the RxJS one. The cornerstone of this API is the **MArbleScheduler** class. Below an example showing how to initiate a scheduler: 
```
MarbleScheduler scheduler = new MarbleScheduler();
``` 
This scheduler can then be used to configure source observables:
```
Observable<String> sourceEvents = scheduler.createColdObservable("a-b-c-|");
```
Then you can use the **MarbleScheduler.expectObservable()** to verify that everything went as expected during the test. 
Below a really simple all-in-one example: 
```
MarbleScheduler scheduler = new MarbleScheduler();
Observable<String> sourceEvents = scheduler.createColdObservable("a-b-c-|"); // create an IObservable<string> emiting 3 "next" events
Observable<String> upperEvents = sourceEvents.select(s -> s.toUpperCase());  // transform the events - this is what we often call the SUT ;)
scheduler.expectObservable(upperEvents).toBe("A-B-C-|");                     // check that the output events have the timing and values as expected
scheduler.flush();                                                           // let the virtual clock goes... otherwise nothing happens
```
**Important:** as shown above, do not forget to **flush** the scheduler at the end of your test case, otherwise no event will be emitted. 

In the above examples, event values are not specified and string streams are produced (i.e. Observable<String>).  
As with the RxJS api, you can use a parameter map/hash containing event values:
```
Map<String, Integer> values = new HashMap<>();
values.put("a", 1);
values.put("b", 2);
values.put("c", 3);
Observable<Integer> events = scheduler.CreateHotObservable<int>("a-b-c-|", values);
```
In order to reduce the amount of boilerplate code needed to create an iniate the map, you can use guava's ImmutableMap or **MapHelper** static methods:
```
import static rx.marble.MapHelper.of;
...
Observable<Integer> events 
  = scheduler.CreateHotObservable<int>("a-b-c-|", of("a", 1, "b", 2, "c", 3));
```


## Marble ASCII syntax

The syntax remains exactly the same as the one of RxJS.   
Each ASCII character represents what happens during a time interval, by default 10 ticks.  
**'-'** means that nothing happens  
**'a'** or any letter means that an event occurs  
**'|'** means the stream end successfully  
**'#'** means an error occurs

So "a-b-|" means:

- At 0, an event 'a' occurs
- Nothing till 20 where an event 'b' occurs
- Then the stream ends at 40

If some events occurs simultanously, you can group them using paranthesis.  
So "--(abc)--" means events a, b and c occur at time 20.  

For an exhaustive description of the syntax you can checkout 
the [official RxJS documentation](https://github.com/ReactiveX/rxjs/blob/master/doc/writing-marble-tests.md)

## Advanced features

For a complete listof supported features you can checkout 
the [tests of the MarbleScheduler class](https://github.com/alexvictoor/MarbleTest4J/blob/master/src/test/java/rx/marble/MarbleSchedulerTest.java).