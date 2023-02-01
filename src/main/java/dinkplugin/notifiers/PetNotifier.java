package dinkplugin.notifiers;

import com.google.common.collect.ImmutableSet;
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
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static dinkplugin.notifiers.CollectionNotifier.COLLECTION_LOG_REGEX;

@Singleton
public class PetNotifier extends BaseNotifier {

    @VisibleForTesting
    static final Pattern PET_REGEX = Pattern.compile("You (?:have a funny feeling like you|feel something weird sneaking).*");

    @VisibleForTesting
    static final Pattern CLAN_REGEX = Pattern.compile("\\b(?<user>[\\w\\s]+) (?:has a funny feeling like .+ followed|feels something weird sneaking into .+ backpack): (?<pet>.+) at\\s");

    private static final Pattern UNTRADEABLE_REGEX = Pattern.compile("Untradeable drop: (.+)");
    private static final Set<String> PET_NAMES;
    private static final String PRIMED_NAME = "";

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
        if (isEnabled()) {
            if (petName == null) {
                if (PET_REGEX.matcher(chatMessage).matches()) {
                    // Prime the notifier to trigger next tick
                    this.petName = PRIMED_NAME;
                }
            } else if (PRIMED_NAME.equals(petName)) {
                Matcher matcher = Optional.of(UNTRADEABLE_REGEX.matcher(chatMessage))
                    .filter(Matcher::find)
                    .orElseGet(() -> {
                        Matcher m = COLLECTION_LOG_REGEX.matcher(chatMessage);
                        return m.find() ? m : null;
                    });
                if (matcher != null) {
                    String item = matcher.group(1);
                    if (item.startsWith("Pet ") || PET_NAMES.contains(Utils.ucFirst(item))) {
                        this.petName = item;
                    }
                }
            } else {
                // ignore; we already know the pet name
            }
        }
    }

    public void onClanNotification(String message) {
        if (!PRIMED_NAME.equals(petName)) {
            // We have not received the normal message about a pet drop, so this clan message cannot be relevant to us
            return;
        }

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

    static {
        PET_NAMES = ImmutableSet.of(
            "Abyssal orphan",
            "Abyssal protector",
            "Baby chinchompa",
            "Baby mole",
            "Beaver",
            "Bloodhound",
            "Callisto cub",
            "Cat",
            "Chompy chick",
            "Giant squirrel",
            "Hellcat",
            "Hellpuppy",
            "Herbi",
            "Heron",
            "Ikkle hydra",
            "Jal-nib-rek",
            "Kalphite princess",
            "Muphin",
            "Lil' creator",
            "Lil' zik",
            "Little nightmare",
            "Nexling",
            "Noon",
            "Olmlet",
            "Phoenix",
            "Prince black dragon",
            "Rift guardian",
            "Rock golem",
            "Rocky",
            "Scorpia's offspring",
            "Skotos",
            "Smolcano",
            "Sraracha",
            "Tangleroot",
            "Tiny tempor",
            "Toy cat",
            "Tumeken's guardian",
            "Tzrek-jad",
            "Venenatis spiderling",
            "Vet'ion jr.",
            "Vorki",
            "Youngllef"
        );
    }
}
