package dinkplugin.notifiers;

import dinkplugin.DinkPlugin;
import dinkplugin.DinkPluginConfig;
import dinkplugin.Utils;
import dinkplugin.message.NotificationBody;

import javax.inject.Inject;
import java.util.function.Function;

public abstract class BaseNotifier {
    protected final DinkPlugin plugin;

    @Inject
    BaseNotifier(DinkPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isEnabled() {
        return !Utils.isIgnoredWorld(plugin.getClient().getWorldType());
    }

    protected final void createMessage(Function<DinkPluginConfig, Boolean> sendImage, NotificationBody<?> body) {
        plugin.getMessageHandler().createMessage(sendImage.apply(plugin.getConfig()), body);
    }
}
