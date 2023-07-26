package dinkplugin.notifiers;

import com.google.common.collect.ImmutableSet;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.message.templating.Replacements;
import dinkplugin.message.templating.Template;
import dinkplugin.notifiers.data.PetNotificationData;
import dinkplugin.util.ItemSearcher;
import dinkplugin.util.ItemUtils;
import dinkplugin.util.Utils;
import lombok.AccessLevel;
import lombok.Setter;
import net.runelite.api.annotations.Varbit;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static dinkplugin.notifiers.CollectionNotifier.COLLECTION_LOG_REGEX;

@Singleton
public class PetNotifier extends BaseNotifier {

    @Varbit
    public static final int LOOT_DROP_NOTIFICATIONS = 5399;

    @Varbit
    public static final int UNTRADEABLE_LOOT_DROPS = 5402;

    public static final String UNTRADEABLE_WARNING = "Pet Notifier cannot reliably identify pet names unless you enable the game setting: Untradeable loot notifications";

    @VisibleForTesting
    static final Pattern PET_REGEX = Pattern.compile("You (?:have a funny feeling like you|feel something weird sneaking).*");

    @VisibleForTesting
    static final Pattern CLAN_REGEX = Pattern.compile("\\b(?<user>[\\w\\s]+) (?:has a funny feeling like .+ followed|feels something weird sneaking into .+ backpack): (?<pet>.+) at (?<milestone>.+)");

    private static final Pattern UNTRADEABLE_REGEX = Pattern.compile("Untradeable drop: (.+)");
    private static final Set<String> PET_NAMES;
    private static final String PRIMED_NAME = "";

    /**
     * The maximum number ticks to wait for {@link #milestone} to be populated,
     * before firing notification with only the {@link #petName}.
     *
     * @see #ticksWaited
     */
    @VisibleForTesting
    static final int MAX_TICKS_WAIT = 5;

    /**
     * Tracks the number of ticks that occur where {@link #milestone} is not populated
     * while {@link #petName} <i>is</i> populated.
     *
     * @see #onTick()
     */
    private final AtomicInteger ticksWaited = new AtomicInteger();

    @Inject
    private ItemSearcher itemSearcher;

    @Setter(AccessLevel.PRIVATE)
    private volatile String petName = null;

    private volatile String milestone = null;

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
                parseItemFromGameMessage(chatMessage)
                    .filter(item -> item.startsWith("Pet ") || PET_NAMES.contains(Utils.ucFirst(item)))
                    .ifPresent(this::setPetName);
            } else {
                // ignore; we already know the pet name
            }
        }
    }

    public void onClanNotification(String message) {
        if (petName == null) {
            // We have not received the normal message about a pet drop, so this clan message cannot be relevant to us
            return;
        }

        Matcher matcher = CLAN_REGEX.matcher(message);
        if (matcher.find()) {
            String user = matcher.group("user").trim();
            if (user.equals(Utils.getPlayerName(client))) {
                this.petName = matcher.group("pet");
                this.milestone = StringUtils.removeEnd(matcher.group("milestone"), ".");
            }
        }
    }

    public void onTick() {
        if (petName == null)
            return;

        if (milestone != null || ticksWaited.incrementAndGet() > MAX_TICKS_WAIT)
            this.handleNotify();
    }

    public void reset() {
        this.petName = null;
        this.milestone = null;
        this.ticksWaited.set(0);
    }

    private void handleNotify() {
        Template notifyMessage = Template.builder()
            .template(config.petNotifyMessage())
            .replacementBoundary("%")
            .replacement("%USERNAME%", Replacements.ofText(Utils.getPlayerName(client)))
            .build();

        String thumbnail = Optional.ofNullable(petName)
            .filter(s -> !s.isEmpty())
            .map(Utils::ucFirst)
            .map(itemSearcher::findItemId)
            .map(ItemUtils::getItemImageUrl)
            .orElse(null);

        PetNotificationData extra = new PetNotificationData(StringUtils.defaultIfEmpty(petName, null), milestone);

        createMessage(config.petSendImage(), NotificationBody.builder()
            .extra(extra)
            .text(notifyMessage)
            .thumbnailUrl(thumbnail)
            .type(NotificationType.PET)
            .build());

        reset();
    }

    private static Optional<String> parseItemFromGameMessage(String message) {
        Matcher untradeableMatcher = UNTRADEABLE_REGEX.matcher(message);
        if (untradeableMatcher.find()) {
            return Optional.of(untradeableMatcher.group(1));
        }

        Matcher collectionMatcher = COLLECTION_LOG_REGEX.matcher(message);
        if (collectionMatcher.find()) {
            return Optional.of(collectionMatcher.group("itemName"));
        }

        return Optional.empty();
    }

    static {
        // Note: We don't explicitly list out names that have the "Pet " prefix
        // since they are matched by filter(item -> item.startsWith("Pet ")) above
        PET_NAMES = ImmutableSet.of(
            "Abyssal orphan",
            "Abyssal protector",
            "Baby chinchompa",
            "Baby mole",
            "Baron",
            "Beaver",
            "Bloodhound",
            "Callisto cub",
            "Chompy chick",
            "Giant squirrel",
            "Hellcat",
            "Hellpuppy",
            "Herbi",
            "Heron",
            "Ikkle hydra",
            "Jal-nib-rek",
            "Kalphite princess",
            "Lil' creator",
            "Lil' zik",
            "Little nightmare",
            "Muphin",
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
            "Tumeken's guardian",
            "Tzrek-jad",
            "Venenatis spiderling",
            "Vet'ion jr.",
            "Vorki",
            "Youngllef"
        );
    }
}
