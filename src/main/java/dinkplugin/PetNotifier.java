package dinkplugin;

import javax.inject.Inject;

public class PetNotifier extends BaseNotifier {

    @Inject
    public PetNotifier(DinkPlugin plugin) {
        super(plugin);
    }

    @Override
    public void handleNotify() {
        String notifyMessage = plugin.config.petNotifyMessage()
            .replaceAll("%USERNAME%", Utils.getPlayerName());
        NotificationBody<Object> body = new NotificationBody<>();
        body.setContent(notifyMessage);
        body.setType(NotificationType.PET);
        plugin.messageHandler.createMessage(plugin.config.petSendImage(), body);
    }
}
