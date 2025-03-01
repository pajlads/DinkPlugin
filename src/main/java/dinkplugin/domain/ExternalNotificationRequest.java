package dinkplugin.domain;

import dinkplugin.message.Field;
import dinkplugin.message.templating.impl.SimpleReplacement;
import lombok.Data;
import okhttp3.HttpUrl;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
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
    private @Nullable List<HttpUrl> urls;

    public String getUrls(Supplier<String> defaultValue) {
        return urls != null
            ? urls.stream().filter(Objects::nonNull).map(HttpUrl::toString).collect(Collectors.joining("\n"))
            : defaultValue.get();
    }

    public List<Field> getFields() {
        return this.fields != null ? this.fields : Collections.emptyList();
    }

}
