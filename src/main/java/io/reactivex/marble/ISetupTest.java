package io.reactivex.marble;


import java.util.Map;

public interface ISetupTest {

    void toBe(String marble,
              Map<String, ?> values,
              Exception errorValue);

    void toBe(String marble,
              Map<String, ?> values);

    void toBe(String marble);

}
