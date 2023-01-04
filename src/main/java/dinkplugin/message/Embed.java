package dinkplugin.message;

import com.google.gson.annotations.JsonAdapter;
import dinkplugin.util.ColorAdapter;
import dinkplugin.util.InstantAdapter;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.awt.Color;
import java.time.Instant;
import java.util.List;

@Value
@Builder
public class Embed {
    public static final int MAX_DESCRIPTION_LENGTH = 4096;
    public static final int MAX_FOOTER_LENGTH = 2048;

    String title;
    String description;
    Author author;
    @JsonAdapter(ColorAdapter.class)
    Color color;
    UrlEmbed image;
    UrlEmbed thumbnail;
    @Singular
    List<Field> fields;
    Footer footer;
    @JsonAdapter(InstantAdapter.class)
    Instant timestamp;

    public static Embed ofImage(String url) {
        return Embed.builder()
            .image(new UrlEmbed(url))
            .build();
    }

    @Value
    public static class UrlEmbed {
        String url;
    }
}
