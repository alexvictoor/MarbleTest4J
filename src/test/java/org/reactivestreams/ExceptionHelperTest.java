package org.reactivestreams;


import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ExceptionHelperTest {

    @Test
    public void should_return_line_outside_callee() {
        String caller = new Dummy().findCaller();
        assertThat(caller).contains("should_return_line_outside_callee");
    }

    @Test
    public void should_return_line_outside_callees() {
        String caller = new Foo().findCaller();
        assertThat(caller).contains("should_return_line_outside_callees");
    }

    public static class Dummy {
        public String findCaller() {
            return ExceptionHelper.findCallerInStackTrace(getClass());
        }
    }

    public static class Foo {
        public String findCaller() {
            return new Bar().findCaller(getClass());
        }
    }

    public static class Bar {
        public String findCaller(Class clazz) {
            return ExceptionHelper.findCallerInStackTrace(clazz, getClass());
        }
    }
}