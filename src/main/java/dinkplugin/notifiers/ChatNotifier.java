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
            this.handleNotify(messageType, source, message);
        }
    }

    private void handleNotify(ChatMessageType type, String source, String message) {
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
            .extra(new ChatNotificationData(type, source, message))
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
}
