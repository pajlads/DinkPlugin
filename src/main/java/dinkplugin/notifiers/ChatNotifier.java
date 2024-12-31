package dinkplugin.notifiers;

import dinkplugin.domain.ChatNotificationType;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.message.templating.Replacements;
import dinkplugin.message.templating.Template;
import dinkplugin.notifiers.data.ChatNotificationData;
import dinkplugin.util.ConfigUtil;
import dinkplugin.util.Utils;
import lombok.Synchronized;
import net.runelite.api.ChatMessageType;
import net.runelite.api.GameState;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanChannelMember;
import net.runelite.api.clan.ClanID;
import net.runelite.api.clan.ClanSettings;
import net.runelite.api.clan.ClanTitle;
import net.runelite.api.events.CommandExecuted;
import net.runelite.client.events.NotificationFired;
import net.runelite.client.util.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Singleton;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Singleton
public class ChatNotifier extends BaseNotifier {
    public static final String PATTERNS_CONFIG_KEY = "chatPatterns";

    private final Collection<Pattern> regexps = new CopyOnWriteArrayList<>();

    @Override
    public boolean isEnabled() {
        return config.notifyChat() && super.isEnabled();
    }

    @Override
    protected String getWebhookUrl() {
        return config.chatWebhook();
    }

    public void init() {
        this.loadPatterns(config.chatPatterns());
    }

    @Synchronized
    public void reset() {
        regexps.clear();
    }

    public void onConfig(String key, String value) {
        if (PATTERNS_CONFIG_KEY.equals(key)) {
            this.loadPatterns(value);
        }
    }

    public void onMessage(@NotNull ChatMessageType messageType, @Nullable String source, @NotNull String message) {
        ChatNotificationType type = ChatNotificationType.MAPPINGS.get(messageType);
        if (type != null && config.chatMessageTypes().contains(type) && isEnabled() && hasMatch(message)) {
            String cleanSource = source != null ? Text.sanitize(source) : null;
            this.handleNotify(messageType, cleanSource, message);
        }
    }

    public void onCommand(CommandExecuted event) {
        if (config.chatMessageTypes().contains(ChatNotificationType.COMMAND) && isEnabled()) {
            String fullMessage = join(event);
            if (hasMatch(fullMessage)) {
                this.handleNotify(ChatMessageType.UNKNOWN, "CommandExecuted", fullMessage);
            }
        }
    }

    public void onNotification(NotificationFired event) {
        var types = config.chatMessageTypes();
        if (event.getNotification().isGameMessage() && client.getGameState() == GameState.LOGGED_IN && types.contains(ChatNotificationType.GAME)) {
            return; // avoid duplicate notification (since runelite will also post to chat)
        }
        if (types.contains(ChatNotificationType.RUNELITE) && isEnabled() && hasMatch(event.getMessage())) {
            this.handleNotify(ChatMessageType.UNKNOWN, "NotificationFired", event.getMessage());
        }
    }

    private void handleNotify(ChatMessageType type, String source, String message) {
        var clanTitle = getClanTitle(type, source, message);
        String playerName = Utils.getPlayerName(client);
        Template template = Template.builder()
            .template(config.chatNotifyMessage())
            .replacementBoundary("%")
            .replacement("%USERNAME%", Replacements.ofText(playerName))
            .replacement("%MESSAGE%", Replacements.ofText(message))
            .build();
        createMessage(config.chatSendImage(), NotificationBody.builder()
            .text(template)
            .type(NotificationType.CHAT)
            .extra(new ChatNotificationData(type, source, clanTitle, message))
            .playerName(playerName)
            .build());
    }

    private boolean hasMatch(String chatMessage) {
        for (Pattern pattern : regexps) {
            if (pattern.matcher(chatMessage).find())
                return true;
        }
        return false;
    }

    @Synchronized
    private void loadPatterns(String configValue) {
        regexps.clear();
        regexps.addAll(
            ConfigUtil.readDelimited(configValue)
                .map(Utils::regexify)
                .collect(Collectors.toList())
        );
    }

    @Nullable
    private ClanTitle getClanTitle(@NotNull ChatMessageType type, @Nullable String source, @NotNull String message) {
        if (type == ChatMessageType.CLAN_MESSAGE && message.endsWith(" has joined.")) {
            String name = message.substring(0, message.length() - " has joined.".length());
            var title = getClanTitle(ChatMessageType.CLAN_CHAT, name);
            return title != null ? title : getClanTitle(ChatMessageType.CLAN_GUEST_CHAT, name);
        }
        return getClanTitle(type, source);
    }

    @Nullable
    private ClanTitle getClanTitle(@NotNull ChatMessageType type, @Nullable String name) {
        if (name == null) return null;

        ClanChannel channel;
        ClanSettings settings;
        if (type == ChatMessageType.CLAN_CHAT) {
            channel = client.getClanChannel();
            settings = client.getClanSettings();
        } else if (type == ChatMessageType.CLAN_GUEST_CHAT) {
            channel = client.getGuestClanChannel();
            settings = client.getGuestClanSettings();
        } else if (type == ChatMessageType.CLAN_GIM_CHAT) {
            channel = client.getClanChannel(ClanID.GROUP_IRONMAN);
            settings = client.getClanSettings(ClanID.GROUP_IRONMAN);
        } else {
            channel = null;
            settings = null;
        }

        ClanChannelMember member;
        if (channel == null || settings == null || (member = channel.findMember(name)) == null) {
            return null;
        }
        return settings.titleForRank(member.getRank());
    }

    private static String join(CommandExecuted event) {
        StringBuilder sb = new StringBuilder();
        sb.append("::").append(event.getCommand());

        String[] args = event.getArguments();
        if (args != null) {
            for (String arg : args) {
                sb.append(' ').append(arg);
            }
        }
        return sb.toString();
    }
}
