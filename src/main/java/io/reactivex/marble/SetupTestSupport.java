package io.reactivex.marble;


import java.util.Map;

public abstract class SetupTestSupport implements ISetupTest {

    public void toBe(String marble,
              Map<String, ?> values) {

        toBe(marble, values, null);
    }

    public void toBe(String marble) {
        toBe(marble, null);
    }

}
