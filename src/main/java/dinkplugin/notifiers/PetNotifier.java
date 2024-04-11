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
import lombok.Value;
import net.runelite.api.Varbits;
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

    private volatile boolean duplicate = false;

    private volatile boolean backpack = false;

    private volatile boolean collection = false;

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
                    this.duplicate = chatMessage.contains("would have been");
                    this.backpack = chatMessage.contains(" backpack");
                }
            } else if (PRIMED_NAME.equals(petName) || !collection) {
                parseItemFromGameMessage(chatMessage)
                    .filter(item -> item.getItemName().startsWith("Pet ") || PET_NAMES.contains(Utils.ucFirst(item.getItemName())))
                    .ifPresent(parseResult -> {
                        setPetName(parseResult.getItemName());
                        if (parseResult.isCollectionLog()) {
                            this.collection = true;
                        }
                    });
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

        if (milestone != null || ticksWaited.incrementAndGet() > MAX_TICKS_WAIT) {
            // ensure notifier was not disabled during wait ticks
            if (isEnabled()) {
                this.handleNotify();
            }
            this.reset();
        }
    }

    public void reset() {
        this.petName = null;
        this.milestone = null;
        this.duplicate = false;
        this.backpack = false;
        this.collection = false;
        this.ticksWaited.set(0);
    }

    private void handleNotify() {
        Boolean previouslyOwned;
        if (duplicate) {
            previouslyOwned = true;
        } else if (client.getVarbitValue(Varbits.COLLECTION_LOG_NOTIFICATION) % 2 == 1) {
            // when collection log chat notification is enabled, presence or absence of notification indicates ownership history
            previouslyOwned = !collection;
        } else {
            previouslyOwned = null;
        }

        String gameMessage;
        if (backpack) {
            gameMessage = "feels something weird sneaking into their backpack";
        } else if (previouslyOwned != null && previouslyOwned) {
            gameMessage = "has a funny feeling like they would have been followed...";
        } else {
            gameMessage = "has a funny feeling like they're being followed";
        }

        Template notifyMessage = Template.builder()
            .template(config.petNotifyMessage())
            .replacementBoundary("%")
            .replacement("%USERNAME%", Replacements.ofText(Utils.getPlayerName(client)))
            .replacement("%GAME_MESSAGE%", Replacements.ofText(gameMessage))
            .build();

        String thumbnail = Optional.ofNullable(petName)
            .filter(s -> !s.isEmpty())
            .map(Utils::ucFirst)
            .map(itemSearcher::findItemId)
            .map(ItemUtils::getItemImageUrl)
            .orElse(null);

        PetNotificationData extra = new PetNotificationData(StringUtils.defaultIfEmpty(petName, null), milestone, duplicate, previouslyOwned);

        createMessage(config.petSendImage(), NotificationBody.builder()
            .extra(extra)
            .text(notifyMessage)
            .thumbnailUrl(thumbnail)
            .type(NotificationType.PET)
            .build());
    }

    private static Optional<ParseResult> parseItemFromGameMessage(String message) {
        Matcher untradeableMatcher = UNTRADEABLE_REGEX.matcher(message);
        if (untradeableMatcher.find()) {
            return Optional.of(new ParseResult(untradeableMatcher.group(1), false));
        }

        Matcher collectionMatcher = COLLECTION_LOG_REGEX.matcher(message);
        if (collectionMatcher.find()) {
            return Optional.of(new ParseResult(collectionMatcher.group("itemName"), true));
        }

        return Optional.empty();
    }

    @Value
    private static class ParseResult {
        String itemName;
        boolean collectionLog;
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
            "Butch",
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
            "Lil'viathan",
            "Little nightmare",
            "Muphin",
            "Nexling",
            "Noon",
            "Olmlet",
            "Phoenix",
            "Prince black dragon",
            "Quetzin",
            "Rift guardian",
            "Rock golem",
            "Rocky",
            "Scorpia's offspring",
            "Scurry",
            "Skotos",
            "Smolcano",
            "Smol heredit",
            "Sraracha",
            "Tangleroot",
            "Tiny tempor",
            "Tumeken's guardian",
            "Tzrek-jad",
            "Venenatis spiderling",
            "Vet'ion jr.",
            "Vorki",
            "Wisp",
            "Youngllef"
        );
    }
}
