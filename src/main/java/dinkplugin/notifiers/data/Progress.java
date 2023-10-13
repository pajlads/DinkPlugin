package dinkplugin.notifiers.data;

import lombok.Value;

@Value
public class Progress {
    int completed;
    int total;

    public static Progress of(int completed, int total) {
        if (total <= 0 || completed < 0) return null;
        return new Progress(completed, total);
    }
}
