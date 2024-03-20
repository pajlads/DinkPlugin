package dinkplugin.notifiers;

import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.message.templating.Replacements;
import dinkplugin.message.templating.Template;
import dinkplugin.notifiers.data.PetNotificationData;
import dinkplugin.util.ItemSearcher;
import dinkplugin.util.ItemUtils;
import dinkplugin.util.KillCountService;
import dinkplugin.util.MathUtils;
import dinkplugin.util.Utils;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.Value;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.Varbits;
import net.runelite.api.annotations.Varbit;
import net.runelite.http.api.loottracker.LootRecordType;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.IntToDoubleFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static dinkplugin.notifiers.CollectionNotifier.COLLECTION_LOG_REGEX;
import static java.util.Map.entry;

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
    private static final Map<String, Source> PET_NAMES_TO_SOURCE;
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

    @Inject
    private KillCountService killCountService;

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
                    .filter(item -> item.getItemName().startsWith("Pet ") || PET_NAMES_TO_SOURCE.containsKey(Utils.ucFirst(item.getItemName())))
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

        String pet = petName != null ? Utils.ucFirst(petName) : null;
        String thumbnail = Optional.ofNullable(pet)
            .filter(s -> !s.isEmpty())
            .map(itemSearcher::findItemId)
            .map(ItemUtils::getItemImageUrl)
            .orElse(null);

        Source source = petName != null ? PET_NAMES_TO_SOURCE.get(pet) : null;
        Double rarity = source != null ? source.getProbability(client, killCountService) : null;
        Integer actions = rarity != null ? source.estimateActions(client, killCountService) : null;
        Double luck = actions != null && (previouslyOwned == null || !previouslyOwned)
            ? source.calculateLuck(client, killCountService, rarity, actions) : null;

        PetNotificationData extra = new PetNotificationData(StringUtils.defaultIfEmpty(petName, null), milestone, duplicate, previouslyOwned, rarity, actions, luck);

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

    private static abstract class Source {
        abstract Double getProbability(Client client, KillCountService kcService);

        abstract Integer estimateActions(Client client, KillCountService kcService);

        Double calculateLuck(Client client, KillCountService kcService, double probability, int killCount) {
            return MathUtils.cumulativeGeometric(probability, killCount);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = true)
    private static class SkillSource extends Source {
        Skill skill;
        int baseChance;
        Function<Client, Integer> estimateActions;

        @Override
        Double getProbability(Client client, KillCountService kcService) {
            int level = client.getRealSkillLevel(skill);
            return 1.0 / (baseChance - 25 * level);
        }

        @Override
        Integer estimateActions(Client client, KillCountService kcService) {
            return estimateActions.apply(client);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = true)
    private static class KcSource extends Source {
        String name;
        double probability;

        @Override
        Double getProbability(Client client, KillCountService kcService) {
            return this.probability;
        }

        @Override
        Integer estimateActions(Client client, KillCountService kcService) {
            Integer kc = kcService.getKillCount(LootRecordType.UNKNOWN, name);
            return kc != null && kc > 0 ? kc : null;
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    private static class MultiKcSource extends Source {
        String[] names;
        double[] rates;

        public MultiKcSource(String source1, double prob1, String source2, double prob2) {
            this.names = new String[] { source1, source2 };
            this.rates = new double[] { prob1, prob2 };
        }

        @Override
        Double getProbability(Client client, KillCountService kcService) {
            final int n = names.length;
            int[] counts = new int[n];
            int totalKc = 0;
            for (int i = 0; i < n; i++) {
                Integer kc = kcService.getKillCount(LootRecordType.UNKNOWN, names[i]);
                if (kc == null) continue;
                totalKc += kc;
                counts[i] = kc;
            }
            if (totalKc <= 0) {
                return null;
            }
            double weighted = 0;
            for (int i = 0; i < n; i++) {
                weighted += rates[i] * counts[i] / totalKc;
            }
            return weighted;
        }

        @Override
        Integer estimateActions(Client client, KillCountService kcService) {
            return Arrays.stream(names)
                .map(name -> kcService.getKillCount(LootRecordType.UNKNOWN, name))
                .filter(Objects::nonNull)
                .reduce(Integer::sum)
                .orElse(null);
        }

        @Override
        Double calculateLuck(Client client, KillCountService kcService, double probability, int killCount) {
            double p = 1;
            for (int i = 0; i < names.length; i++) {
                Integer kc = kcService.getKillCount(LootRecordType.UNKNOWN, names[i]);
                if (kc == null) continue;
                p *= Math.pow(1 - rates[i], kc); // similar to geometric distribution survival function
            }
            return 1 - p;
        }
    }

    static {
        PET_NAMES_TO_SOURCE = Map.<String, Source>ofEntries(
            entry("Abyssal orphan", new KcSource("Abyssal Sire", 1.0 / 2_560)),
            entry("Abyssal protector", new Source() {
                @Override
                Double getProbability(Client client, KillCountService kcService) {
                    return 1.0 / 4_000;
                }

                @Override
                Integer estimateActions(Client client, KillCountService kcService) {
                    Integer kc = kcService.getKillCount(LootRecordType.EVENT, "Guardians of the Rift");
                    return kc != null && kc > 0 ? kc * 3 : null;
                }
            }),
            entry("Baby chinchompa", new SkillSource(Skill.HUNTER, 82_758, client -> client.getSkillExperience(Skill.HUNTER) / 315)), // black chinchompas
            entry("Baby mole", new KcSource("Giant Mole", 1.0 / 3_000)),
            entry("Baron", new KcSource("Duke Sucellus", 1.0 / 2_500)),
            entry("Butch", new KcSource("Vardorvis", 1.0 / 3_000)),
            entry("Beaver", new SkillSource(Skill.WOODCUTTING, 264_336, client -> client.getSkillExperience(Skill.WOODCUTTING) / 85)), // teaks
            entry("Bloodhound", new KcSource("Clue Scroll (master)", 1.0 / 1_000)),
            entry("Callisto cub", new MultiKcSource("Callisto", 1.0 / 1_500, "Artio", 1.0 / 2_800)),
            entry("Chompy chick", new KcSource("Chompy bird", 1.0 / 500)),
            entry("Giant squirrel", new Source() {
                private final Map<String, Integer> courses = Map.ofEntries(
                    entry("Gnome Stronghold Agility", 35_609),
                    entry("Shayzien Agility Course", 31_804),
                    entry("Shayzien Advanced Agility Course", 29_738),
                    entry("Agility Pyramid", 9_901),
                    entry("Penguin Agility", 9_779),
                    entry("Barbarian Outpost", 44_376),
                    entry("Agility Arena", 26_404),
                    entry("Ape Atoll Agility", 37_720),
                    entry("Wilderness Agility", 34_666),
                    entry("Werewolf Agility", 32_597),
                    entry("Dorgesh-Kaan Agility Course", 10_561),
                    entry("Prifddinas Agility Course", 25_146),
                    entry("Draynor Village Rooftop", 33_005),
                    entry("Al Kharid Rooftop", 26_648),
                    entry("Varrock Rooftop", 24_410),
                    entry("Canifis Rooftop", 36_842),
                    entry("Falador Rooftop", 26_806),
                    entry("Seers' Village Rooftop", 35_205),
                    entry("Pollnivneach Rooftop", 33_422),
                    entry("Rellekka Rooftop", 31_063),
                    entry("Ardougne Rooftop", 34_440),
                    entry("Hallowed Sepulchre Floor 1", 35_000),
                    entry("Hallowed Sepulchre Floor 2", 16_000),
                    entry("Hallowed Sepulchre Floor 3", 8_000),
                    entry("Hallowed Sepulchre Floor 4", 4_000),
                    entry("Hallowed Sepulchre Floor 5", 2_000)
                );

                @Override
                Double getProbability(Client client, KillCountService kcService) {
                    double[] rates = getRates(client);
                    int[] counts = getCounts(kcService);
                    int total = Arrays.stream(counts).sum();
                    if (total <= 0) return null;
                    return IntStream.range(0, rates.length)
                        .mapToDouble(i -> rates[i] * counts[i] / total)
                        .sum();
                }

                @Override
                Integer estimateActions(Client client, KillCountService kcService) {
                    int total = Arrays.stream(getCounts(kcService)).sum();
                    return total > 0 ? total : null;
                }

                @Override
                Double calculateLuck(Client client, KillCountService kcService, double probability, int killCount) {
                    final double[] rates = getRates(client);
                    final int[] counts = getCounts(kcService);
                    double p = 1;
                    for (int i = 0, n = rates.length; i < n; i++) {
                        p *= Math.pow(1 - rates[i], counts[i]); // see MultiKcSource
                    }
                    return 1 - p;
                }

                private int[] getCounts(KillCountService kcService) {
                    return courses.keySet()
                        .stream()
                        .mapToInt(course -> {
                            Integer count = kcService.getKillCount(LootRecordType.UNKNOWN, course);
                            return count != null && count > 0 ? count + 1 : 0;
                        })
                        .toArray();
                }

                private double[] getRates(Client client) {
                    int level = client.getRealSkillLevel(Skill.AGILITY);
                    IntToDoubleFunction calc = base -> 1.0 / (base - level * 25); // see SkillSource
                    return courses.entrySet()
                        .stream()
                        .mapToDouble(entry -> {
                            if (entry.getKey().startsWith("Hallowed"))
                                return 1.0 / entry.getValue();
                            return calc.applyAsDouble(entry.getValue());
                        })
                        .toArray();
                }
            }),
            entry("Hellpuppy", new KcSource("Cerberus", 1.0 / 3_000)),
            entry("Herbi", new KcSource("Herbiboar", 1.0 / 6_500)),
            entry("Heron", new SkillSource(Skill.FISHING, 257_770, client -> client.getSkillExperience(Skill.FISHING) / 100)), // swordfish
            entry("Ikkle hydra", new KcSource("Alchemical Hydra", 1.0 / 3_000)),
            entry("Jal-nib-rek", new KcSource("TzKal-Zuk", 1.0 / 100)),
            entry("Kalphite princess", new KcSource("Kalphite Queen", 1.0 / 3_000)),
            entry("Lil' creator", new KcSource("Spoils of war", 1.0 / 400)),
            entry("Lil' zik", new KcSource(" Theatre of Blood", 1.0 / 650)), // assume normal mode
            entry("Lil'viathan", new KcSource("The Leviathan", 1.0 / 2_500)),
            entry("Little nightmare", new KcSource("Nightmare", 1.0 / 3_200)), // assume team size 4
            entry("Muphin", new KcSource("Phantom Muspah", 1.0 / 2_500)),
            entry("Nexling", new KcSource("Nex", 1.0 / 500)),
            entry("Noon", new KcSource("Grotesque Guardians", 1.0 / 3_000)),
            entry("Olmlet", new Source() {
                @Override
                Double getProbability(Client client, KillCountService kcService) {
                    // https://oldschool.runescape.wiki/w/Ancient_chest#Unique_drop_table
                    int totalPoints = client.getVarbitValue(Varbits.TOTAL_POINTS);
                    if (totalPoints <= 0) {
                        totalPoints = 26_025;
                    }
                    return 0.01 * (totalPoints / 8_676) / 53;
                }

                @Override
                Integer estimateActions(Client client, KillCountService kcService) {
                    return Stream.of("", " Challenge Mode")
                        .map(suffix -> "Chambers of Xeric" + suffix)
                        .map(event -> kcService.getKillCount(LootRecordType.EVENT, event))
                        .filter(Objects::nonNull)
                        .reduce(Integer::sum)
                        .orElse(null);
                }
            }),
            entry("Pet chaos elemental", new MultiKcSource("Chaos Elemental", 1.0 / 300, "Chaos Fanatic", 1.0 / 1_000)),
            entry("Pet dagannoth prime", new KcSource("Dagannoth Prime", 1.0 / 5_000)),
            entry("Pet dagannoth rex", new KcSource("Dagannoth Rex", 1.0 / 5_000)),
            entry("Pet dagannoth supreme", new KcSource("Dagannoth Supreme", 1.0 / 5_000)),
            entry("Pet dark core", new KcSource("Corporeal Beast", 1.0 / 5_000)),
            entry("Pet general graardor", new KcSource("General Graardor", 1.0 / 5_000)),
            entry("Pet k'ril tsutsaroth", new KcSource("K'ril Tsutsaroth", 1.0 / 5_000)),
            entry("Pet kraken", new KcSource("Kraken", 1.0 / 5_000)),
            entry("Pet penance queen", new Source() {
                @Override
                Double getProbability(Client client, KillCountService kcService) {
                    return 1.0 / 1_000;
                }

                @Override
                Integer estimateActions(Client client, KillCountService kcService) {
                    return client.getVarbitValue(Varbits.BA_GC);
                }
            }),
            entry("Pet smoke devil", new KcSource("Thermonuclear smoke devil", 1.0 / 3_000)),
            entry("Pet snakeling", new KcSource("Zulrah", 1.0 / 4_000)),
            entry("Pet zilyana", new KcSource("Commander Zilyana", 1.0 / 5_000)),
            entry("Phoenix", new KcSource("Wintertodt", 1.0 / 5_000)),
            entry("Prince black dragon", new KcSource("King Black Dragon", 1.0 / 3_000)),
            entry("Rift guardian", new SkillSource(Skill.RUNECRAFT, 1_795_758, client -> client.getSkillExperience(Skill.RUNECRAFT) / 10)), // lava runes
            entry("Rock golem", new SkillSource(Skill.MINING, 211_886, client -> client.getSkillExperience(Skill.MINING) / 65)), // gemstones
            entry("Rocky", new SkillSource(Skill.THIEVING, 36_490, client -> client.getSkillExperience(Skill.THIEVING) / 42)), // stalls
            entry("Scorpia's offspring", new KcSource("Scorpia", 1 / 2015.75)),
            entry("Scurry", new KcSource("Scurrius", 1.0 / 3_000)),
            entry("Skotos", new KcSource("Skotizo", 1.0 / 65)),
            entry("Smolcano", new KcSource("Zalcano", 1.0 / 2_250)),
            entry("Sraracha", new KcSource("Sarachnis", 1.0 / 3_000)),
            entry("Tangleroot", new SkillSource(Skill.FARMING, 7_500, client -> client.getSkillExperience(Skill.FARMING) / 119)), // mushrooms
            entry("Tiny tempor", new KcSource("Reward pool (Tempoross)", 1.0 / 8_000)),
            entry("Tumeken's guardian", new Source() {
                @Override
                Double getProbability(Client client, KillCountService kcService) {
                    // https://oldschool.runescape.wiki/w/Chest_(Tombs_of_Amascut)#Tertiary_rewards
                    int rewardPoints = client.getVarbitValue(Varbits.TOTAL_POINTS);
                    int raidLevels = Math.min(client.getVarbitValue(Varbits.TOA_RAID_LEVEL), 550);
                    int x = Math.min(raidLevels, 400);
                    int y = Math.max(raidLevels - 400, 0);
                    return 0.01 * rewardPoints / (350_000 - 700 * (x + y / 3)); // assume latest is representative
                }

                @Override
                Integer estimateActions(Client client, KillCountService kcService) {
                    return Stream.of("", ": Entry Mode", ": Expert Mode")
                        .map(suffix -> "Tombs of Amascut" + suffix)
                        .map(event -> kcService.getKillCount(LootRecordType.EVENT, event))
                        .filter(Objects::nonNull)
                        .reduce(Integer::sum)
                        .orElse(null);
                }
            }),
            entry("Tzrek-jad", new KcSource("TzTok-Jad", 1.0 / 200)),
            entry("Venenatis spiderling", new MultiKcSource("Venenatis", 1.0 / 1_500, "Spindel", 1.0 / 2_800)),
            entry("Vet'ion jr.", new MultiKcSource("Vet'ion", 1.0 / 1_500, "Calvar'ion", 1.0 / 2_800)),
            entry("Vorki", new KcSource("Vorkath", 1.0 / 3_000)),
            entry("Wisp", new KcSource("The Whisperer", 1.0 / 2_000)),
            entry("Youngllef", new MultiKcSource("Gauntlet", 1.0 / 2_000, "Corrupted Gauntlet", 1.0 / 800))
        );
    }
}
