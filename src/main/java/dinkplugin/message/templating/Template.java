package dinkplugin.message.templating;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Template)) return false;

        // custom equals/hashCode based on outputs (rather than inputs) for ease of testing notifiers
        Template other = (Template) o;
        return this.evaluate(false).equals(other.evaluate(false))
            && this.evaluate(true).equals(other.evaluate(true));
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.evaluate(false), this.evaluate(true));
    }
}
