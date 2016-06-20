package rx.marble;


import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ExceptionHelperTest {

    @Test
    public void should_return_line_outside_callee() {
        String caller = new Dummy().findCaller();
        assertThat(caller).contains("should_return_line_outside_callee");
    }

    public static class Dummy {
        public String findCaller() {
            return ExceptionHelper.findCallerInStackTrace(getClass());
        }
    }
}