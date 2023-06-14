package dinkplugin.message.templating;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

@Value
@Builder
public class Template implements Evaluable {
    @NotNull
    String template;

    @Singular
    Map<String, Evaluable> replacements;

    @Nullable
    String replacementBoundary; // e.g., "%"

    @Override
    public String evaluate(boolean rich) {
        if (replacementBoundary != null) {
            return evaluateFast(rich);
        }
        return evaluateSlow(rich);
    }

    private String evaluateSlow(boolean rich) {
        String message = template;
        for (Map.Entry<String, Evaluable> e : replacements.entrySet()) {
            message = message.replace(e.getKey(), e.getValue().evaluate(rich));
        }
        return message;
    }

    private String evaluateFast(boolean rich) {
        StringBuilder message = new StringBuilder(template);
        int i = message.indexOf(replacementBoundary);
        while (i != -1) {
            int next = message.indexOf(replacementBoundary, i + 1);
            if (next < 0) break;

            int endExclusive = next + 1;
            String key = message.substring(i, endExclusive);
            Evaluable replacement = replacements.get(key);
            if (replacement != null) {
                String evaluated = replacement.evaluate(rich);
                message.replace(i, endExclusive, evaluated);
                i = endExclusive + evaluated.length() - key.length();
            } else {
                i = next;
            }
        }
        return message.toString();
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
