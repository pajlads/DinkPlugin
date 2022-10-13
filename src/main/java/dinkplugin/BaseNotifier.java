package dinkplugin;

import javax.inject.Inject;

public class BaseNotifier {
    protected final DinkPlugin plugin;

    @Inject
    public BaseNotifier(DinkPlugin plugin) {
        this.plugin = plugin;
    }

    public void handleNotify() {
        NotificationBody<Object> b = new NotificationBody<>();
        b.setContent("This is a base notification");
        plugin.messageHandler.createMessage(false, b);
    }
}
