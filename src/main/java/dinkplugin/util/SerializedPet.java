package dinkplugin.util;

import lombok.Value;

import java.util.Map;

@Value
public class SerializedPet implements Sanitizable {
    int itemId;
    String name;

    @Override
    public Map<String, Object> sanitized() {
        return Map.of("itemId", itemId, "name", name);
    }
}
