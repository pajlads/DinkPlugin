package dinkplugin.notifiers;

import dinkplugin.DinkPluginConfig;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.Utils;

import java.util.regex.Pattern;

public class PetNotifier extends BaseNotifier {
    static final Pattern PET_REGEX = Pattern.compile("You (?:have a funny feeling like you|feel something weird sneaking).*");

    @Override
    public boolean isEnabled() {
        return config.notifyPet() && super.isEnabled();
    }

    public void onChatMessage(String chatMessage) {
        if (isEnabled() && PET_REGEX.matcher(chatMessage).matches()) {
            this.handleNotify();
        }
    }

    private void handleNotify() {
        String notifyMessage = config.petNotifyMessage()
            .replace("%USERNAME%", Utils.getPlayerName(client));

        createMessage(DinkPluginConfig::petSendImage, NotificationBody.builder()
            .content(notifyMessage)
            .type(NotificationType.PET)
            .build());
    }
}
