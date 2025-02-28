package dinkplugin.domain;

import dinkplugin.message.Field;
import dinkplugin.message.templating.impl.SimpleReplacement;
import dinkplugin.util.ConfigUtil;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import okhttp3.HttpUrl;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Data
public class ExternalNotificationRequest {

    private String sourcePlugin;
    private String text;
    private boolean imageRequested;
    private @Nullable String title;
    private @Nullable String thumbnail;
    private @Nullable List<Field> fields;
    private @Nullable Map<String, SimpleReplacement> replacements;
    private @Nullable Map<String, Object> metadata;
    @Getter(AccessLevel.PRIVATE) // to avoid accidentally using the requested url without sanitization
    private @Nullable String urls;

    public String getSanitizedUrls() {
        return ConfigUtil.readDelimited(this.urls)
            .map(HttpUrl::parse)
            .filter(Objects::nonNull)
            .filter(url -> "discord.com".equalsIgnoreCase(url.host()))
            .map(HttpUrl::toString)
            .collect(Collectors.joining("\n"));
    }

    public List<Field> getFields() {
        return this.fields != null ? this.fields : Collections.emptyList();
    }

}
