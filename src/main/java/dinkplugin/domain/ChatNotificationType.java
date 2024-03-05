package dinkplugin.domain;

import net.runelite.api.ChatMessageType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static net.runelite.api.ChatMessageType.*;

/**
 * A condensed version of {@link ChatMessageType} for more user-friendly configuration.
 */
public enum ChatNotificationType {
    GAME("Game Engine", GAMEMESSAGE, ENGINE, MESBOX, LOGINLOGOUTNOTIFICATION, PLAYERRELATED),
    CLAN("Clan Notifications", CLAN_MESSAGE, CLAN_GIM_MESSAGE, CLAN_GUEST_MESSAGE, FRIENDSCHATNOTIFICATION),
    TRADES("Trades and Duels", TRADE, TRADE_SENT, TRADEREQ, CHALREQ_TRADE, CHALREQ_FRIENDSCHAT, CHALREQ_CLANCHAT),
    CHAT("User Messages (Discouraged)", PUBLICCHAT, PRIVATECHAT, PRIVATECHATOUT, MODCHAT, MODPRIVATECHAT, FRIENDSCHAT, CLAN_CHAT, CLAN_GUEST_CHAT, AUTOTYPER, MODAUTOTYPER);

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
