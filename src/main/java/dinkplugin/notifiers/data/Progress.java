package dinkplugin.notifiers.data;

import dinkplugin.util.Sanitizable;
import lombok.Value;

import java.util.Map;

@Value
public class Progress implements Sanitizable {
    int completed;
    int total;

    @Override
    public Map<String, Object> sanitized() {
        return Map.of("completed", completed, "total", total);
    }

    public static Progress of(int completed, int total) {
        if (total <= 0 || completed < 0) return null;
        return new Progress(completed, total);
    }
}
