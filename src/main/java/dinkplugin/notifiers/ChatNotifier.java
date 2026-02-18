package dinkplugin.notifiers;

import dinkplugin.domain.ChatNotificationType;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.message.templating.Replacements;
import dinkplugin.message.templating.Template;
import dinkplugin.notifiers.data.ChatNotificationData;
import dinkplugin.util.Utils;
import net.runelite.api.ChatMessageType;
import net.runelite.api.GameState;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanChannelMember;
import net.runelite.api.clan.ClanID;
import net.runelite.api.clan.ClanSettings;
import net.runelite.api.clan.ClanTitle;
import net.runelite.api.events.CommandExecuted;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.events.NotificationFired;
import net.runelite.client.util.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static dinkplugin.domain.ChatNotificationType.*;

@Singleton
public class ChatNotifier extends BaseNotifier {
    public static final String PATTERNS_CONFIG_KEY = "chatPatterns";

    @Inject
    private ClientThread clientThread;

    private final Collection<Pattern> regexps = new ArrayList<>();
    private volatile boolean dirty;

    @Override
    public boolean isEnabled() {
        return config.notifyChat() && super.isEnabled();
    }

    @Override
    protected String getWebhookUrl() {
        return config.chatWebhook();
    }

    public void init() {
        this.dirty = true;
    }

    public void reset() {
        this.dirty = true;
        clientThread.invoke(regexps::clear);
    }

    public void onConfig(String key) {
        if (PATTERNS_CONFIG_KEY.equals(key)) {
            this.dirty = true;
        }
    }

    public void onTick() {
        if (this.dirty) {
            var username = Utils.getPlayerName(client);
            if (username == null || username.isEmpty()) {
                return;
            }
            this.dirty = false;
            this.loadPatterns(username);
        }
    }

    public void onMessage(@NotNull ChatMessageType messageType, @Nullable String source, @NotNull String message) {
        ChatNotificationType type = ChatNotificationType.MAPPINGS.get(messageType);
        if (type != null && config.chatMessageTypes().contains(type) && isEnabled() && hasMatch(message)) {
            String cleanSource = source != null ? Text.sanitize(source) : null;
            this.handleNotify(type, messageType, cleanSource, message);
        }
    }

    public void onCommand(CommandExecuted event) {
        if (config.chatMessageTypes().contains(COMMAND) && isEnabled()) {
            String fullMessage = join(event);
            if (hasMatch(fullMessage)) {
                this.handleNotify(COMMAND, ChatMessageType.UNKNOWN, "CommandExecuted", fullMessage);
            }
        }
    }

    public void onNotification(NotificationFired event) {
        var types = config.chatMessageTypes();
        if (event.getNotification().isGameMessage() && client.getGameState() == GameState.LOGGED_IN && types.contains(GAME)) {
            return; // avoid duplicate notification (since runelite will also post to chat)
        }
        if (types.contains(RUNELITE) && isEnabled() && hasMatch(event.getMessage())) {
            this.handleNotify(RUNELITE, ChatMessageType.UNKNOWN, "NotificationFired", event.getMessage());
        }
    }

    private void handleNotify(ChatNotificationType dinkType, ChatMessageType type, String source, String message) {
        var clanTitle = getClanTitle(type, source, message);
        String playerName = Utils.getPlayerName(client);
        Template template = Template.builder()
            .template(config.chatNotifyMessage())
            .replacementBoundary("%")
            .replacement("%USERNAME%", Replacements.ofText(playerName))
            .replacement("%MESSAGE%", Replacements.ofText(message))
            .replacement("%SENDER%", Replacements.ofText(getSender(dinkType, source)))
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

    private void loadPatterns(String username) {
        regexps.clear();
        regexps.addAll(
            config.chatPatterns().lines()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.replace("%USERNAME%", username))
                .map(Utils::regexify)
                .filter(Objects::nonNull)
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

    private static String getSender(ChatNotificationType type, String source) {
		if (source == null || source.isEmpty() || type == COMMAND || type == RUNELITE) {
			return "[" + type + "]";
		}
		return source;
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
