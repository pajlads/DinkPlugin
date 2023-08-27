package dinkplugin.message;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

/**
 * @see <a href="https://discord.com/developers/docs/reference#error-messages">Error Message Format</a>
 */
@Data
@Setter(AccessLevel.PRIVATE)
class DiscordErrorMessage {
    /**
     * @see <a href="https://discord.com/developers/docs/topics/opcodes-and-status-codes#json">Possible Error Codes</a>
     */
    int code;
    String message;
}
