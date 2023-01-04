package dinkplugin.message;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Value
@Builder
public class Author {
    @NotNull String name;
    @Nullable
    @SerializedName("icon_url")
    String iconUrl;
}
