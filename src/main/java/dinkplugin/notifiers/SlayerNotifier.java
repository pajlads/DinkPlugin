package dinkplugin.notifiers;

import dinkplugin.DinkPlugin;
import dinkplugin.DinkPluginConfig;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.Utils;
import dinkplugin.notifiers.data.SlayerNotificationData;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SlayerNotifier extends BaseNotifier {
    public static final Pattern SLAYER_TASK_REGEX = Pattern.compile("You have completed your task! You killed (?<task>[\\d,]+ [^.]+)\\..*");
    private static final Pattern SLAYER_COMPLETE_REGEX = Pattern.compile("You've completed (?:at least )?(?<taskCount>[\\d,]+) (?:Wilderness )?tasks?(?: and received (?<points>\\d+) points, giving you a total of [\\d,]+|\\.You'll be eligible to earn reward points if you complete tasks from a more advanced Slayer Master\\.| and reached the maximum amount of Slayer points \\((?<points2>[\\d,]+)\\))?");

    private String slayerTask = "";
    private int badTicks = 0; // used to prevent notifs from using stale data

    @Inject
    public SlayerNotifier(DinkPlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean isEnabled() {
        return plugin.getConfig().notifySlayer() && super.isEnabled();
    }

    public void onChatMessage(String chatMessage) {
        if (isEnabled()
            && (chatMessage.contains("Slayer master")
            || chatMessage.contains("Slayer Master")
            || chatMessage.contains("completed your task!")
        )) {
            Matcher taskMatcher = SLAYER_TASK_REGEX.matcher(chatMessage);
            if (taskMatcher.find()) {
                this.slayerTask = taskMatcher.group("task");
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
        if (!slayerTask.isEmpty())
            badTicks++;

        // Clear data if 2 ticks pass with only partial parsing
        if (badTicks > 1)
            reset();
    }

    private void handleNotify(String slayerPoints, String slayerCompleted) {
        if (slayerPoints.isEmpty() || slayerTask.isEmpty() || slayerCompleted.isEmpty()) {
            return;
        }

        int threshold = plugin.getConfig().slayerPointThreshold();
        if (threshold <= 0 || Integer.parseInt(slayerPoints) >= threshold) {
            String notifyMessage = StringUtils.replaceEach(
                plugin.getConfig().slayerNotifyMessage(),
                new String[] { "%USERNAME%", "%TASK%", "%TASKCOUNT%", "%POINTS%" },
                new String[] { Utils.getPlayerName(plugin.getClient()), slayerTask, slayerCompleted, slayerPoints }
            );

            createMessage(DinkPluginConfig::slayerSendImage, NotificationBody.builder()
                .content(notifyMessage)
                .extra(new SlayerNotificationData(slayerTask, slayerCompleted, slayerPoints))
                .type(NotificationType.SLAYER)
                .build());
        }

        this.reset();
    }

    private void reset() {
        slayerTask = "";
        badTicks = 0;
    }
}
