package dinkplugin.util;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.Duration;
import java.time.format.DateTimeParseException;

public class DurationAdapter extends TypeAdapter<Duration> {
    @Override
    public void write(JsonWriter out, Duration duration) throws IOException {
        out.value(duration != null ? duration.toString() : null);
    }

    @Override
    public Duration read(JsonReader in) throws IOException {
        if (in.hasNext()) {
            String str = in.nextString();
            try {
                return Duration.parse(str);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }
}
