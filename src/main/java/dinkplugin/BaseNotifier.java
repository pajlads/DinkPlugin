package dinkplugin;

import javax.inject.Inject;

public class BaseNotifier {
    protected final DinkPlugin plugin;

    @Inject
    public BaseNotifier(DinkPlugin plugin) {
        this.plugin = plugin;
    }

    public void handleNotify() {
        plugin.messageHandler.createMessage("This is a base notification", false, null);
    }
}
