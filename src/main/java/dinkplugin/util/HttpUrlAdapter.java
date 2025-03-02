package dinkplugin.util;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import okhttp3.HttpUrl;

import java.io.IOException;

public class HttpUrlAdapter extends TypeAdapter<HttpUrl> {
    @Override
    public void write(JsonWriter out, HttpUrl url) throws IOException {
        out.value(url != null ? url.toString() : null);
    }

    @Override
    public HttpUrl read(JsonReader in) throws IOException {
        return in.hasNext() ? HttpUrl.parse(in.nextString()) : null;
    }
}
