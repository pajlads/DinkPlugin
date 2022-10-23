package dinkplugin.notifiers;

import dinkplugin.DinkPlugin;
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
    public void handleNotify() {
        String notifyMessage = plugin.getConfig().petNotifyMessage()
            .replaceAll("%USERNAME%", Utils.getPlayerName());
        NotificationBody<Object> body = new NotificationBody<>();
        body.setContent(notifyMessage);
        body.setType(NotificationType.PET);
        messageHandler.createMessage(plugin.getConfig().petSendImage(), body);
    }

    public void onChatMessage(String chatMessage) {
        if (plugin.getConfig().notifyPet() && PET_REGEX.matcher(chatMessage).matches()) {
            this.handleNotify();
        }
    }
}
