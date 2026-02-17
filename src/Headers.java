import java.util.*;

class Headers {
    private final Map<String, List<String>> _headers = new HashMap<>();

    // it would be easier to extend Map than use separate class in this case
    Map<String, List<String>> toMap() {
        return Map.copyOf(_headers);
    }

    void set(String key, String[] values) {
        this._headers.put(key, Arrays.asList(values));
    }

    void set(String key, String value) {
        List<String> values = new ArrayList<>();
        values.add(value);

        this._headers.put(key, values);
    }

    void add(String key, String[] newValues) {
        _headers.computeIfAbsent(key, k -> new ArrayList<>())
                .addAll(Arrays.asList(newValues));
    }

    void add(String key, String value) {
        _headers.computeIfAbsent(key, k -> new ArrayList<>())
                .add(value);
    }

    String[] get(String key) {
        return _headers.getOrDefault(key, Collections.emptyList()).toArray(String[]::new);
    }

    String getFirst(String key) {
        return _headers.getOrDefault(key, Collections.emptyList()).getFirst();
    }
}
