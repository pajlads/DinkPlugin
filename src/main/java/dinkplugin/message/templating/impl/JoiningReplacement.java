package dinkplugin.message.templating.impl;

import dinkplugin.message.templating.Evaluable;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.Iterator;
import java.util.List;

@Value
@Builder
public class JoiningReplacement implements Evaluable {
    @Singular
    List<Evaluable> components;
    @Builder.Default
    String delimiter = "";

    @Override
    public String evaluate(boolean rich) {
        if (components.isEmpty())
            return "";

        StringBuilder sb = new StringBuilder();
        Iterator<Evaluable> it = components.iterator();
        sb.append(it.next().evaluate(rich));
        while (it.hasNext()) {
            sb.append(delimiter);
            sb.append(it.next().evaluate(rich));
        }
        return sb.toString();
    }
}
