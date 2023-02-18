package dinkplugin.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Describes a Discord rich embed field
 *
 * @see <a href="https://birdie0.github.io/discord-webhooks-guide/structure/embed/fields.html">Example</a>
 */
@Value
@Builder
@AllArgsConstructor
public class Field {
    @NotNull String name;
    @NotNull String value;
    @Nullable Boolean inline;

    public Field(String name, String value) {
        this(name, value, true);
    }

    public static String formatBlock(String codeBlockLanguage, String content) {
        return String.format("```%s\n%s\n```", StringUtils.defaultString(codeBlockLanguage), content);
    }
}
