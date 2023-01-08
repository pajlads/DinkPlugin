package dinkplugin.util;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeParseException;

/**
 * Serialized and deserializes {@link Instant} instances
 * from their equivalent ISO-8601 string representation.
 * <p>
 * Discord requires this string format, rather than epoch milliseconds
 * that the default RuneLite GSON instance would produce.
 */
public class InstantAdapter extends TypeAdapter<Instant> {
    @Override
    public void write(JsonWriter out, Instant instant) throws IOException {
        out.value(instant != null ? instant.toString() : null);
    }

    @Override
    public Instant read(JsonReader in) throws IOException {
        if (in.hasNext()) {
            try {
                return Instant.parse(in.nextString());
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }
}
