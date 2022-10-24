package dinkplugin.notifiers;

import dinkplugin.DinkPlugin;
import dinkplugin.DinkPluginConfig;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.Utils;

import javax.inject.Inject;
import java.util.regex.Pattern;

public class PetNotifier extends BaseNotifier {
    static final Pattern PET_REGEX = Pattern.compile("You (?:have a funny feeling like you|feel something weird sneaking).*");

    @Inject
    public PetNotifier(DinkPlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean isEnabled() {
        return plugin.getConfig().notifyPet() && super.isEnabled();
    }

    private void handleNotify() {
        String notifyMessage = plugin.getConfig().petNotifyMessage()
            .replaceAll("%USERNAME%", Utils.getPlayerName());

        createMessage(DinkPluginConfig::petSendImage, NotificationBody.builder()
            .content(notifyMessage)
            .type(NotificationType.PET)
            .build());
    }

    public void onChatMessage(String chatMessage) {
        if (isEnabled() && PET_REGEX.matcher(chatMessage).matches()) {
            this.handleNotify();
        }
    }
}
