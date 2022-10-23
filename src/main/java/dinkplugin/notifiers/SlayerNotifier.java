package dinkplugin.notifiers;

import dinkplugin.DinkPlugin;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.Utils;
import dinkplugin.notifiers.data.SlayerNotificationData;

import javax.inject.Inject;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SlayerNotifier extends BaseNotifier {
    public static final Pattern SLAYER_TASK_REGEX = Pattern.compile("You have completed your task! You killed (?<task>[\\d,]+ [^.]+)\\..*");
    private static final Pattern SLAYER_COMPLETE_REGEX = Pattern.compile("You've completed (?:at least )?(?<taskCount>[\\d,]+) (?:Wilderness )?tasks?(?: and received (?<points>\\d+) points, giving you a total of [\\d,]+|\\.You'll be eligible to earn reward points if you complete tasks from a more advanced Slayer Master\\.| and reached the maximum amount of Slayer points \\((?<points2>[\\d,]+)\\))?");

    private String slayerTask = "";
    private String slayerPoints = "";
    private String slayerCompleted = "";

    @Inject
    public SlayerNotifier(DinkPlugin plugin) {
        super(plugin);
    }

    @Override
    public void handleNotify() {
        if (plugin.isIgnoredWorld()) return;
        // Little jank, but it's a bit cleaner than having bools and checking in the main plugin class
        if (slayerPoints.isEmpty() || slayerTask.isEmpty() || slayerCompleted.isEmpty()) {
            return;
        }

        String notifyMessage = plugin.getConfig().slayerNotifyMessage()
            .replaceAll("%USERNAME%", Utils.getPlayerName())
            .replaceAll("%TASK%", slayerTask)
            .replaceAll("%TASKCOUNT%", slayerCompleted)
            .replaceAll("%POINTS%", slayerPoints);
        NotificationBody<SlayerNotificationData> body = new NotificationBody<>();
        SlayerNotificationData extra = new SlayerNotificationData();
        extra.setSlayerPoints(slayerPoints);
        extra.setSlayerCompleted(slayerCompleted);
        extra.setSlayerTask(slayerTask);
        body.setExtra(extra);
        body.setContent(notifyMessage);
        body.setType(NotificationType.SLAYER);
        messageHandler.createMessage(plugin.getConfig().slayerSendImage(), body);

        slayerTask = "";
        slayerPoints = "";
        slayerCompleted = "";
    }

    public void onChatMessage(String chatMessage) {

        if (plugin.getConfig().notifySlayer()
            && (chatMessage.contains("Slayer master")
            || chatMessage.contains("Slayer Master")
            || chatMessage.contains("completed your task!")
        )) {
            Matcher taskMatcher = SLAYER_TASK_REGEX.matcher(chatMessage);
            Matcher pointsMatcher = SLAYER_COMPLETE_REGEX.matcher(chatMessage);

            if (taskMatcher.find()) {
                this.slayerTask = taskMatcher.group("task");
                this.handleNotify();
            }

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
                this.slayerPoints = slayerPoints;
                this.slayerCompleted = slayerTasksCompleted;

                this.handleNotify();
            }
        }
    }
}
