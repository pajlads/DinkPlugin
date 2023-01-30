package dinkplugin.notifiers;

import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.notifiers.data.PetNotificationData;
import dinkplugin.util.ItemSearcher;
import dinkplugin.util.ItemUtils;
import dinkplugin.util.Utils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Singleton
public class PetNotifier extends BaseNotifier {
    @VisibleForTesting
    static final Pattern PET_REGEX = Pattern.compile("You (?:have a funny feeling like you|feel something weird sneaking).*");

    @VisibleForTesting
    static final Pattern CLAN_REGEX = Pattern.compile("\\b(?<user>[\\w\\s]+) (?:has a funny feeling like .+ followed|feels something weird sneaking into .+ backpack): (?<pet>.+) at\\s");

    @Inject
    private ItemSearcher itemSearcher;

    private String petName = null;

    @Override
    public boolean isEnabled() {
        return config.notifyPet() && super.isEnabled();
    }

    @Override
    protected String getWebhookUrl() {
        return config.petWebhook();
    }

    public void onChatMessage(String chatMessage) {
        if (petName == null && isEnabled() && PET_REGEX.matcher(chatMessage).matches()) {
            // Prime the notifier to trigger next tick
            this.petName = "";
        }
    }

    public void onClanNotification(String message) {
        // Clan message can only come after the normal pet message
        if (petName == null)
            return;

        Matcher matcher = CLAN_REGEX.matcher(message);
        if (matcher.find()) {
            String user = matcher.group("user").trim();
            if (user.equals(Utils.getPlayerName(client))) {
                this.petName = matcher.group("pet");
            }
        }
    }

    public void onTick() {
        if (petName != null)
            this.handleNotify();
    }

    public void reset() {
        this.petName = null;
    }

    private void handleNotify() {
        String notifyMessage = config.petNotifyMessage()
            .replace("%USERNAME%", Utils.getPlayerName(client));

        String thumbnail = Optional.ofNullable(petName)
            .filter(s -> !s.isEmpty())
            .map(Utils::ucFirst)
            .map(itemSearcher::findItemId)
            .map(ItemUtils::getItemImageUrl)
            .orElse(null);

        PetNotificationData extra = new PetNotificationData(StringUtils.defaultIfEmpty(petName, null));

        createMessage(config.petSendImage(), NotificationBody.builder()
            .extra(extra)
            .text(notifyMessage)
            .thumbnailUrl(thumbnail)
            .type(NotificationType.PET)
            .build());

        reset();
    }
}
