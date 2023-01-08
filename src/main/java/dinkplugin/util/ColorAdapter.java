package dinkplugin.util;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.awt.Color;
import java.io.IOException;

/**
 * Serializes and deserializes {@link Color} instances
 * from their decimal representation.
 * <p>
 * Discord requires this decimal format, rather than a hex string
 * that the default RuneLite GSON instance would produce.
 */
public class ColorAdapter extends TypeAdapter<Color> {
    @Override
    public void write(JsonWriter out, Color color) throws IOException {
        out.value(color != null ? color.getRGB() ^ 0xFF000000 : null);
    }

    @Override
    public Color read(JsonReader in) throws IOException {
        if (in.hasNext()) {
            return new Color(in.nextInt());
        }
        return null;
    }
}
