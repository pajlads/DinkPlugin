package dinkplugin.util;

import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.events.InteractingChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.slayer.SlayerPlugin;
import net.runelite.client.plugins.slayer.SlayerPluginService;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.ref.WeakReference;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Singleton
public class SlayerService {

    public static final Pattern BOSS_REGEX = Pattern.compile("You are granted .+ Slayer XP for completing your boss task against(?: the)? (?<name>.+)\\.$");
    public static final Pattern SLAYER_TASK_REGEX = Pattern.compile("You have completed your task! You killed (?<task>[\\d,]+ [^.]+)\\..*");
    public static final Pattern SLAYER_COMPLETE_REGEX = Pattern.compile("You've completed (?:at least )?(?<taskCount>[\\d,]+) (?:Wilderness )?tasks?(?: and received (?<points>[\\d,]+) points, giving you a total of [\\d,]+|\\.You'll be eligible to earn reward points if you complete tasks from a more advanced Slayer Master\\.| and reached the maximum amount of Slayer points \\((?<points2>[\\d,]+)\\))?");
    public static final Pattern TASK_MONSTER_REGEX = Pattern.compile("^(?<count>\\d*)\\s*(?<monster>.+)$");

    private static final String RL_PLUGIN_CLASS_NAME = SlayerPlugin.class.getSimpleName().toLowerCase();

    @Inject
    private Client client;

    @Inject
    private ConfigManager configManager;

    @Getter
    @Inject
    private SlayerPluginService runeliteService;

    @Inject
    private ScheduledExecutorService executor;

    private boolean hasTask = false;
    private WeakReference<NPC> slayerTarget = new WeakReference<>(null);

    public void reset() {
        this.hasTask = false;
        this.slayerTarget.clear();
    }

    public void onGameMessage(String chatMessage) {
        if (SLAYER_TASK_REGEX.matcher(chatMessage).matches() || SLAYER_COMPLETE_REGEX.matcher(chatMessage).matches()) {
            executor.schedule(this::reset, 30, TimeUnit.SECONDS);
        }
    }

    public void onInteraction(InteractingChanged event) {
        if (event.getSource() != client.getLocalPlayer()) return;
        if (!(event.getTarget() instanceof NPC)) return;

        this.hasTask = StringUtils.isNotEmpty(runeliteService.getTask());
        if (!hasTask) return;

        NPC npc = (NPC) event.getTarget();
        if (runeliteService.getTargets().contains(npc)) {
            this.slayerTarget = new WeakReference<>(npc);
        }
    }

    public Optional<Boolean> isTaskActive() {
        return ConfigUtil.isPluginDisabled(configManager, RL_PLUGIN_CLASS_NAME)
            ? Optional.empty()
            : Optional.of(this.hasTask);
    }

    public Optional<String> getTargetName() {
        NPC npc = slayerTarget.get();
        if (npc == null) return Optional.empty();
        return Optional.ofNullable(npc.getName());
    }

}
