package dinkplugin;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class DiscordMessageBody {
    private String content;
    private List<Embed> embeds = new ArrayList<>();

    @Data
    static class Embed {
        final UrlEmbed image;
    }

    @Data
    static class UrlEmbed {
        final String url;
    }
}
