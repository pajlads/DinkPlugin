package dinkplugin.util;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.awt.Color;
import java.io.IOException;

public class ColorAdapter extends TypeAdapter<Color> {
    @Override
    public void write(JsonWriter out, Color color) throws IOException {
        out.value(color != null ? color.getRGB() : null);
    }

    @Override
    public Color read(JsonReader in) throws IOException {
        if (in.hasNext()) {
            return new Color(in.nextInt());
        }
        return null;
    }
}
