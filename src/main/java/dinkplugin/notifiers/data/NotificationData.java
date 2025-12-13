package dinkplugin.notifiers.data;

import dinkplugin.message.Field;
import dinkplugin.util.Sanitizable;

import java.util.Collections;
import java.util.List;

public abstract class NotificationData implements Sanitizable {
    public List<Field> getFields() {
        return Collections.emptyList();
    }
}
