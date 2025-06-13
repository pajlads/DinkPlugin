package dinkplugin.notifiers;

import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.message.templating.Replacements;
import dinkplugin.message.templating.Template;
import dinkplugin.notifiers.data.SpeedrunNotificationData;
import dinkplugin.util.QuestUtils;
import dinkplugin.util.TimeUtils;
import dinkplugin.util.Utils;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;

import java.time.Duration;

@Slf4j
public class SpeedrunNotifier extends BaseNotifier {
    private boolean isPersonalBest = false;

    @Override
    public boolean isEnabled() {
        // intentionally doesn't call super as WorldUtils.isIgnoredWorld includes speedrunning
        return config.notifySpeedrun() && accountTracker.hasValidState();
    }

    @Override
    protected String getWebhookUrl() {
        return config.speedrunWebhook();
    }

    public void reset() {
        isPersonalBest = false;
    }

    public void onWidgetLoaded(WidgetLoaded event) {
        if (event.getGroupId() == InterfaceID.QUESTSCROLL_SPEEDRUN && isEnabled()) {
            Widget questName = client.getWidget(InterfaceID.QuestscrollSpeedrun.QUEST_TITLE);
            Widget duration = client.getWidget(InterfaceID.QuestscrollSpeedrun.TIME_TEXT);
            Widget personalBest = client.getWidget(InterfaceID.QuestscrollSpeedrun.BEST_TEXT);
            if (questName != null && duration != null && personalBest != null) {
                this.attemptNotify(QuestUtils.parseQuestWidget(questName.getText()), duration.getText(), personalBest.getText());
            } else {
                log.warn("Found speedrun finished widget (group id {}) but it is missing something, questName={}, duration={}, pb={}", event.getGroupId(), questName, duration, personalBest);
            }
        }
    }

    private void attemptNotify(String questName, String duration, String pb) {
        if (!isPersonalBest && config.speedrunPBOnly()) {
            return;
        }

        // pb or notifying on non-pb; take the right string and format placeholders
        Template notifyMessage = Template.builder()
            .template(isPersonalBest ? config.speedrunPBMessage() : config.speedrunMessage())
            .replacementBoundary("%")
            .replacement("%USERNAME%", Replacements.ofText(Utils.getPlayerName(client)))
            .replacement("%QUEST%", Replacements.ofWiki(questName))
            .replacement("%TIME%", Replacements.ofText(duration))
            .replacement("%BEST%", Replacements.ofText(pb))
            .build();

        // Reformat the durations for the extra object
        Duration bestTime = TimeUtils.parseTime(pb);
        Duration currentTime = TimeUtils.parseTime(duration);

        createMessage(config.speedrunSendImage(), NotificationBody.builder()
            .text(notifyMessage)
            .extra(new SpeedrunNotificationData(questName, bestTime.toString(), currentTime.toString(), isPersonalBest))
            .type(NotificationType.SPEEDRUN)
            .build());
        this.reset();
    }

    public void onGameMessage(String chatMessage) {
        if (!isEnabled()) {
            return;
        }

        if (chatMessage.startsWith("Speedrun duration: ")) {
            isPersonalBest = chatMessage.endsWith(" (new personal best)");
        }
    }
}
