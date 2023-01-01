package dinkplugin.message;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Value
@Builder
@AllArgsConstructor
public class Author {
    @NotNull String name;
    @Nullable String url;
    @Nullable
    @SerializedName("icon_url")
    String iconUrl;

    public Author(String name) {
        this(name, null, null);
    }
}
