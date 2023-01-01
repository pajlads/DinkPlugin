package dinkplugin.message;

import com.google.gson.annotations.JsonAdapter;
import dinkplugin.util.ColorAdapter;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.awt.Color;
import java.util.List;

@Value
@Builder
public class Embed {
    String title;
    String description;
    String url;
    Author author;
    @JsonAdapter(ColorAdapter.class)
    Color color;
    UrlEmbed image;
    UrlEmbed thumbnail;
    @Singular
    List<Field> fields;
    Footer footer;

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
