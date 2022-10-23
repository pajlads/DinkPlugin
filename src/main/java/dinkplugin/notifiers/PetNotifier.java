package dinkplugin.notifiers;

import dinkplugin.DinkPlugin;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.Utils;

import javax.inject.Inject;

public class PetNotifier extends BaseNotifier {

    @Inject
    public PetNotifier(DinkPlugin plugin) {
        super(plugin);
    }

    @Override
    public void handleNotify() {
        String notifyMessage = config.petNotifyMessage()
            .replaceAll("%USERNAME%", Utils.getPlayerName());
        NotificationBody<Object> body = new NotificationBody<>();
        body.setContent(notifyMessage);
        body.setType(NotificationType.PET);
        messageHandler.createMessage(config.petSendImage(), body);
    }
}
