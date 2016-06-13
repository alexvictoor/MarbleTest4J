package rx.marble;


import java.util.Map;

public interface ISetupTest {

    void toBe(String marble,
              Map<String, Object> values,
              Exception errorValue);

    void toBe(String marble,
              Map<String, Object> values);

    void toBe(String marble);

}
