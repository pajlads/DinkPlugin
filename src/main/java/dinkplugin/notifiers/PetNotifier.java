package dinkplugin.notifiers;

import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.util.Utils;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.regex.Pattern;

public class PetNotifier extends BaseNotifier {
    @VisibleForTesting
    static final Pattern PET_REGEX = Pattern.compile("You (?:have a funny feeling like you|feel something weird sneaking).*");

    @Override
    public boolean isEnabled() {
        return config.notifyPet() && super.isEnabled();
    }

    @Override
    protected String getWebhookUrl() {
        return config.petWebhook();
    }

    public void onChatMessage(String chatMessage) {
        if (isEnabled() && PET_REGEX.matcher(chatMessage).matches()) {
            this.handleNotify();
        }
    }

    private void handleNotify() {
        String notifyMessage = config.petNotifyMessage()
            .replace("%USERNAME%", Utils.getPlayerName(client));

        createMessage(config.petSendImage(), NotificationBody.builder()
            .content(notifyMessage)
            .type(NotificationType.PET)
            .build());
    }
}
