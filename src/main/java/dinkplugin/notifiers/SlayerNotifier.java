package dinkplugin.notifiers;

import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.message.templating.Evaluable;
import dinkplugin.message.templating.Replacements;
import dinkplugin.message.templating.Template;
import dinkplugin.message.templating.impl.JoiningReplacement;
import dinkplugin.util.Utils;
import dinkplugin.notifiers.data.SlayerNotificationData;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import javax.inject.Singleton;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Singleton
public class SlayerNotifier extends BaseNotifier {
    private static final Pattern BOSS_REGEX = Pattern.compile("You are granted .+ Slayer XP for completing your boss task against(?: the)? (?<name>.+)\\.$");
    @VisibleForTesting
    static final Pattern SLAYER_TASK_REGEX = Pattern.compile("You have completed your task! You killed (?<task>[\\d,]+ [^.]+)\\..*");
    private static final Pattern SLAYER_COMPLETE_REGEX = Pattern.compile("You've completed (?:at least )?(?<taskCount>[\\d,]+) (?:Wilderness )?tasks?(?: and received (?<points>[\\d,]+) points, giving you a total of [\\d,]+|\\.You'll be eligible to earn reward points if you complete tasks from a more advanced Slayer Master\\.| and reached the maximum amount of Slayer points \\((?<points2>[\\d,]+)\\))?");
    private static final Pattern TASK_MONSTER_REGEX = Pattern.compile("^(?<count>\\d*)\\s*(?<monster>.+)$");

    private final AtomicReference<String> slayerTask = new AtomicReference<>("");
    private final AtomicInteger badTicks = new AtomicInteger(); // used to prevent notifs from using stale data

    @Override
    public boolean isEnabled() {
        return config.notifySlayer() && super.isEnabled();
    }

    @Override
    protected String getWebhookUrl() {
        return config.slayerWebhook();
    }

    public void onChatMessage(String chatMessage) {
        if (isEnabled()) {
            if (slayerTask.get().isEmpty()) {
                Matcher bossMatcher = BOSS_REGEX.matcher(chatMessage);
                if (bossMatcher.find()) {
                    String name = bossMatcher.group("name");
                    this.slayerTask.set(name.endsWith(" boss") ? name.substring(0, name.length() - " boss".length()) : name);
                    return;
                }
            }

            Matcher taskMatcher = SLAYER_TASK_REGEX.matcher(chatMessage);
            if (taskMatcher.find()) {
                String task = taskMatcher.group("task");
                slayerTask.getAndUpdate(old -> {
                    if (old == null || old.isEmpty())
                        return task;
                    return String.format("%s %s", task.substring(0, task.indexOf(' ')), old);
                });
                return;
            }

            if (slayerTask.get().isEmpty()) {
                return;
            }

            Matcher pointsMatcher = SLAYER_COMPLETE_REGEX.matcher(chatMessage);
            if (pointsMatcher.find()) {
                String slayerPoints = pointsMatcher.group("points");
                String slayerTasksCompleted = pointsMatcher.group("taskCount");

                if (slayerPoints == null) {
                    slayerPoints = pointsMatcher.group("points2");
                }

                // 3 different cases of seeing points, so in our worst case it's 0
                if (slayerPoints == null) {
                    slayerPoints = "0";
                }

                this.handleNotify(slayerPoints, slayerTasksCompleted);
            }
        }
    }

    public void onTick() {
        // Track how many ticks occur where we only have partial slayer data
        if (!slayerTask.get().isEmpty())
            badTicks.getAndIncrement();

        // Clear data if 2 ticks pass with only partial parsing
        if (badTicks.get() > 1)
            reset();
    }

    private void handleNotify(String slayerPoints, String slayerCompleted) {
        String task = slayerTask.get();
        if (task.isEmpty() || slayerPoints.isEmpty() || slayerCompleted.isEmpty()) {
            return;
        }

        int threshold = config.slayerPointThreshold();
        if (threshold <= 0 || Integer.parseInt(slayerPoints.replace(",", "")) >= threshold) {
            Optional<Pair<Integer, String>> parsedTask = parseTask(task);
            Integer marginalKillCount = parsedTask.map(Pair::getLeft).orElse(null);
            String monster = parsedTask.map(Pair::getRight).orElse(null);

            Template notifyMessage = Template.builder()
                .template(config.slayerNotifyMessage())
                .replacementBoundary("%")
                .replacement("%USERNAME%", Replacements.ofText(Utils.getPlayerName(client)))
                .replacement("%TASK%", buildTask(task, monster, marginalKillCount))
                .replacement("%TASKCOUNT%", Replacements.ofText(slayerCompleted))
                .replacement("%POINTS%", Replacements.ofText(slayerPoints))
                .build();

            createMessage(config.slayerSendImage(), NotificationBody.builder()
                .text(notifyMessage)
                .extra(new SlayerNotificationData(task, slayerCompleted, slayerPoints, marginalKillCount, monster))
                .type(NotificationType.SLAYER)
                .build());
        }

        this.reset();
    }

    public void reset() {
        slayerTask.set("");
        badTicks.set(0);
    }

    @NotNull
    private static Evaluable buildTask(@NotNull String rawTask, @Nullable String monster, @Nullable Integer count) {
        if (count == null || monster == null)
            return Replacements.ofText(rawTask);

        return JoiningReplacement.builder()
            .component(Replacements.ofText(String.valueOf(count)))
            .delimiter(" ")
            .component(Replacements.ofWiki(monster))
            .build();
    }

    @NotNull
    private static Optional<Pair<Integer, String>> parseTask(@NotNull String task) {
        Matcher m = TASK_MONSTER_REGEX.matcher(task);
        if (!m.find()) return Optional.empty();
        return Optional.of(Pair.of(Integer.parseInt(m.group("count")), m.group("monster")));
    }
}
