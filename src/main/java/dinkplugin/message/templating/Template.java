package dinkplugin.message.templating;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@Value
@Builder
public class Template implements Evaluable {
    @NotNull
    String template;

    @Singular
    Map<String, Evaluable> replacements;

    @Override
    public String evaluate(boolean rich) {
        String message = template;
        for (Map.Entry<String, Evaluable> e : replacements.entrySet()) {
            message = message.replace(e.getKey(), e.getValue().evaluate(rich));
        }
        return message;
    }
}
