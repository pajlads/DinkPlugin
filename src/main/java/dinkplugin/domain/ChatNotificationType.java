package dinkplugin.domain;

import net.runelite.api.ChatMessageType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A condensed version of {@link ChatMessageType} for more user-friendly configuration.
 */
public enum ChatNotificationType {
    GAME("Game Engine", ChatMessageType.GAMEMESSAGE, ChatMessageType.ENGINE, ChatMessageType.MESBOX),
    CLAN("Clan Notifications", ChatMessageType.CLAN_MESSAGE, ChatMessageType.CLAN_GIM_MESSAGE, ChatMessageType.CLAN_GUEST_MESSAGE, ChatMessageType.FRIENDSCHATNOTIFICATION),
    TRADE("Trades and Duels", ChatMessageType.TRADE, ChatMessageType.TRADE_SENT, ChatMessageType.TRADEREQ,
        ChatMessageType.CHALREQ_TRADE, ChatMessageType.CHALREQ_FRIENDSCHAT, ChatMessageType.CHALREQ_CLANCHAT);

    public static final Map<ChatMessageType, ChatNotificationType> MAPPINGS;

    private final String displayName;
    private final ChatMessageType[] types;

    ChatNotificationType(String displayName, ChatMessageType... types) {
        this.displayName = displayName;
        this.types = types;
    }

    @Override
    public String toString() {
        return this.displayName;
    }

    static {
        Map<ChatMessageType, ChatNotificationType> map = new HashMap<>();
        for (ChatNotificationType value : values()) {
            for (ChatMessageType type : value.types) {
                map.put(type, value);
            }
        }
        MAPPINGS = Collections.unmodifiableMap(map);
    }
}
