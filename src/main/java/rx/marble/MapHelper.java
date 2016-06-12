package rx.marble;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper inspired from guava's ImmutableMap.of() factory method
 */
public class MapHelper {

    public static <V> Map<String, V> of(String k, V v) {
        return Collections.singletonMap(k,v);
    }

    public static <V> Map<String, V> of(String k1, V v1, String k2, V v2) {
        Map<String, V> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        return map;
    }

    public static <V> Map<String, V> of(String k1, V v1, String k2, V v2, String k3, V v3) {
        Map<String, V> result = of(k1, v1, k2, v2);
        result.put(k3, v3);
        return result;
    }

    public static <V> Map<String, V> of(String k1, V v1, String k2, V v2, String k3, V v3, String k4, V v4) {
        Map<String, V> result = of(k1, v1, k2, v2, k3, v3);
        result.put(k4, v4);
        return result;
    }

    public static <V> Map<String, V> of(String k1, V v1, String k2, V v2, String k3, V v3, String k4, V v4, String k5, V v5) {
        Map<String, V> result = of(k1, v1, k2, v2, k3, v3, k4, v4);
        result.put(k5, v5);
        return result;
    }
}
