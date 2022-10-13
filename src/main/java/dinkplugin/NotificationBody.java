package dinkplugin;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class NotificationBody<T> {
    private NotificationType type;
    private T extra;
    private String playerName;

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
