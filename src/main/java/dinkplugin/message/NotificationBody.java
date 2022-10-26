package dinkplugin.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;

import java.util.LinkedList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationBody<T> {
    private NotificationType type;
    private T extra;
    private String content;
    private String playerName;
    @Builder.Default
    private List<Embed> embeds = new LinkedList<>();

    @Value
    public static class Embed {
        UrlEmbed image;
    }

    @Value
    public static class UrlEmbed {
        String url;
    }
}
