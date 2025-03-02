package dinkplugin.message.templating.impl;

import dinkplugin.message.templating.Evaluable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
