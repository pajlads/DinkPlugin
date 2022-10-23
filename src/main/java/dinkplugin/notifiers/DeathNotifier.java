package dinkplugin.notifiers;

import dinkplugin.DinkPlugin;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.Utils;
import net.runelite.api.events.ActorDeath;

import javax.inject.Inject;

public class DeathNotifier extends BaseNotifier {

    @Inject
    public DeathNotifier(DinkPlugin plugin) {
        super(plugin);
    }

    @Override
    public void handleNotify() {
        if (plugin.isIgnoredWorld()) return;
        String notifyMessage = plugin.getConfig().deathNotifyMessage()
            .replaceAll("%USERNAME%", Utils.getPlayerName());
        NotificationBody<Object> b = new NotificationBody<>();
        b.setContent(notifyMessage);
        b.setType(NotificationType.DEATH);
        messageHandler.createMessage(plugin.getConfig().deathSendImage(), b);
    }

    public void onActorDeath(ActorDeath actor) {
        if (plugin.getConfig().notifyDeath() && plugin.getClient().getLocalPlayer() == actor.getActor()) {
            this.handleNotify();
        }
    }
}
