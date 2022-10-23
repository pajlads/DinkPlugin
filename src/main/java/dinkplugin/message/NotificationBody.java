package dinkplugin.message;

import lombok.Data;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;

@Data
public class NotificationBody<T> {
    private NotificationType type;
    private T extra;
    private String playerName;

    private String content;
    private List<Embed> embeds = new ArrayList<>();

    @Value
    public static class Embed {
        UrlEmbed image;
    }

    @Value
    public static class UrlEmbed {
        String url;
    }
}
