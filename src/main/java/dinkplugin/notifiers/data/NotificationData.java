package dinkplugin.notifiers.data;

import dinkplugin.message.Field;
import dinkplugin.message.Fieldable;

import java.util.Collections;
import java.util.List;

public abstract class NotificationData implements Fieldable {
    @Override
    public List<Field> getFields() {
        return Collections.emptyList();
    }
}
