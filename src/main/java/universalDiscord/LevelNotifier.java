package universalDiscord;

import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Hashtable;

@Slf4j
public class LevelNotifier extends BaseNotifier {

    private ArrayList<String> levelledSkills = new ArrayList<String>();
    private Hashtable<String, Integer> currentLevels = new Hashtable<String, Integer>();
    private boolean sendMessage = false;
    private int ticksWaited = 0;

    @Inject
    public LevelNotifier(DinkPlugin plugin) {
        super(plugin);
    }

    public void reset() {
        currentLevels.clear();
        levelledSkills.clear();
    }

    private boolean checkLevelInterval(int level) {
        return plugin.config.levelInterval() <= 1
                || level == 99
                || level % plugin.config.levelInterval() == 0;
    }

    public void onTick() {
        if(!sendMessage) {
            return;
        }

        ticksWaited++;
        // We wait a couple extra ticks so we can ensure that we process all the levels of the previous tick
        if (ticksWaited > 2) {
            ticksWaited = 0;
            attemptNotify();
        }
    }

    public void attemptNotify() {
        sendMessage = false;
        StringBuilder skillMessage = new StringBuilder();
        int index = 0;

        for (String skill : levelledSkills) {
            if(index == levelledSkills.size()) {
                skillMessage.append(" and ");
            } else if (index > 0) {
                skillMessage.append(", ");
            }
            skillMessage.append(String.format("%s to %s", skill, currentLevels.get(skill)));
            index++;
        }

        String skillString = skillMessage.toString();
        levelledSkills.clear();
        String fullNotification = plugin.config.levelNotifyMessage()
                .replaceAll("%USERNAME%", Utils.getPlayerName())
                .replaceAll("%SKILL%", skillString);
        plugin.messageHandler.createMessage(fullNotification, plugin.config.levelSendImage(), null);
    }

    public void handleLevelUp(String skill, int level) {
        if(checkLevelInterval(level) && currentLevels.get(skill) != null) {
            if(level == currentLevels.get(skill)) {
                return;
            }
            levelledSkills.add(skill);
            sendMessage = true;
        }
        currentLevels.put(skill, level);
    }
}
