package dinkplugin.message.templating.impl;

import dinkplugin.message.templating.Evaluable;
import lombok.*;
import org.jetbrains.annotations.Nullable;

@Data
@Setter(AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
public class SimpleReplacement implements Evaluable {

    private String value;

    @Nullable
    private String richValue;

    @Override
    public String evaluate(boolean rich) {
        return rich && richValue != null ? richValue : value;
    }

}
