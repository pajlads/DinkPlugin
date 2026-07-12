package dinkplugin.message.templating.impl;

import dinkplugin.message.templating.Evaluable;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.Collection;
import java.util.Iterator;

@Value
@Builder
public class JoiningReplacement implements Evaluable {
    @Singular
    Collection<Evaluable> components;
    @Builder.Default
    String delimiter = "";
    @Builder.Default
    String prefix = "";
    @Builder.Default
    String postfix = "";

    @Override
    public String evaluate(boolean rich) {
        if (components.isEmpty())
            return "";

        StringBuilder sb = new StringBuilder();
        sb.append(prefix);
        Iterator<Evaluable> it = components.iterator();
        sb.append(it.next().evaluate(rich));
        while (it.hasNext()) {
            sb.append(delimiter);
            sb.append(it.next().evaluate(rich));
        }
        sb.append(postfix);
        return sb.toString();
    }
}
