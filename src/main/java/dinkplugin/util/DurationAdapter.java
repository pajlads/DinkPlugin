package dinkplugin.util;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.Duration;
import java.time.format.DateTimeParseException;

/**
 * Serializes and deserializes {@link Duration} instances
 * from their equivalent ISO-8601 string representation.
 * <p>
 * This adapter exists because GSON does not ship with
 * a module for the Java 8 time API.
 *
 * @see <a href="https://github.com/google/gson/issues/1059">GSON Issue</a>
 */
public class DurationAdapter extends TypeAdapter<Duration> {
    @Override
    public void write(JsonWriter out, Duration duration) throws IOException {
        out.value(duration != null ? duration.toString() : null);
    }

    @Override
    public Duration read(JsonReader in) throws IOException {
        if (in.hasNext()) {
            try {
                return Duration.parse(in.nextString());
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }
}
