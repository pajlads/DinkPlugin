package dinkplugin.notifiers;

import dinkplugin.DinkPlugin;
import dinkplugin.DinkPluginConfig;
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
    public boolean isEnabled() {
        return plugin.getConfig().notifyDeath() && super.isEnabled();
    }

    public void onActorDeath(ActorDeath actor) {
        if (isEnabled() && plugin.getClient().getLocalPlayer() == actor.getActor()) {
            this.handleNotify();
        }
    }

    private void handleNotify() {
        String notifyMessage = plugin.getConfig().deathNotifyMessage()
            .replaceAll("%USERNAME%", Utils.getPlayerName(plugin.getClient()));

        createMessage(DinkPluginConfig::collectionSendImage, NotificationBody.builder()
            .content(notifyMessage)
            .type(NotificationType.DEATH)
            .build());
    }
}
