package dinkplugin.notifiers;

import com.google.inject.testing.fieldbinder.Bind;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.message.templating.Replacements;
import dinkplugin.message.templating.Template;
import dinkplugin.notifiers.data.GambleNotificationData;
import dinkplugin.notifiers.data.SerializedItemStack;
import dinkplugin.util.ItemSearcher;
import net.runelite.api.ItemID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GambleNotifierTest extends MockedNotifierTest {
    private static final int GRANITE_HELM_PRICE = 29_000;
    private static final int DRAGON_CHAINBODY_PRICE = 150_000;
    private static final int ELITE_CLUE_PRICE = 0;

    @Bind
    @InjectMocks
    GambleNotifier notifier;

    @Bind
    @Mock
    ItemSearcher itemSearcher;

    @Override
    @BeforeEach
    protected void setUp() {
        super.setUp();

        when(config.notifyGamble()).thenReturn(true);
        when(config.gambleSendImage()).thenReturn(true);
        when(config.gambleInterval()).thenReturn(10);
        when(config.gambleRareLoot()).thenReturn(true);
        when(config.gambleNotifyMessage()).thenReturn("%USERNAME% has reached %COUNT% high gambles");
        when(config.gambleRareNotifyMessage()).thenReturn("%USERNAME% has received rare loot at gamble count %COUNT%: \n\n%LOOT%");

        mockItem(ItemID.GRANITE_HELM, GRANITE_HELM_PRICE, "Granite helm");
        mockItem(ItemID.DRAGON_CHAINBODY, DRAGON_CHAINBODY_PRICE, "Dragon chainbody");
        mockItem(ItemID.CLUE_SCROLL_ELITE, ELITE_CLUE_PRICE, "Clue scroll (elite)");
        when(itemSearcher.findItemId("Granite helm")).thenReturn(ItemID.GRANITE_HELM);
        when(itemSearcher.findItemId("Dragon chainbody")).thenReturn(ItemID.DRAGON_CHAINBODY);
        when(itemSearcher.findItemId("Clue scroll (elite)")).thenReturn(ItemID.CLUE_SCROLL_ELITE);
    }

    @Test
    void testNotifyInterval() {
        notifier.onMesBoxNotification("Granite helm! High level gamble count: 20.");

        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            true,
            NotificationBody.builder()
                .text(buildTemplate(PLAYER_NAME + " has reached 20 high gambles"))
                .extra(new GambleNotificationData(20, Collections.singletonList(new SerializedItemStack(ItemID.GRANITE_HELM, 1, GRANITE_HELM_PRICE, "Granite helm"))))
                .type(NotificationType.BARBARIAN_ASSAULT_GAMBLE)
                .build()
        );
    }

    @Test
    void testTertiaryLoot() {
        notifier.onMesBoxNotification("Granite helm! Clue scroll (elite)! High level gamble count: 10.");

        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            true,
            NotificationBody.builder()
                .text(buildTemplate(PLAYER_NAME + " has reached 10 high gambles"))
                .extra(new GambleNotificationData(10, Arrays.asList(
                    new SerializedItemStack(ItemID.GRANITE_HELM, 1, GRANITE_HELM_PRICE, "Granite helm"),
                    new SerializedItemStack(ItemID.CLUE_SCROLL_ELITE, 1, ELITE_CLUE_PRICE, "Clue scroll (elite)")
                )))
                .type(NotificationType.BARBARIAN_ASSAULT_GAMBLE)
                .build()
        );
    }

    @Test
    void testIgnoredInterval() {
        notifier.onMesBoxNotification("Watermelon seed (x 50)! High level gamble count: 21.");
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testRareLoot() {
        notifier.onMesBoxNotification("Dragon chainbody! High level gamble count: 11.");

        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            true,
            NotificationBody.builder()
                .text(
                    Template.builder()
                        .template(PLAYER_NAME + " has received rare loot at gamble count 11: \n\n1 x {{dchain}} (150K)")
                        .replacement("{{dchain}}", Replacements.ofWiki("Dragon chainbody"))
                        .build()
                )
                .extra(new GambleNotificationData(11, Collections.singletonList(new SerializedItemStack(ItemID.DRAGON_CHAINBODY, 1, DRAGON_CHAINBODY_PRICE, "Dragon chainbody"))))
                .type(NotificationType.BARBARIAN_ASSAULT_GAMBLE)
                .build()
        );
    }

    @Test
    void testIgnoredRareLootInterval() {
        when(config.gambleRareLoot()).thenReturn(false);
        notifier.onMesBoxNotification("Dragon chainbody! High level gamble count: 13.");
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testDisabled() {
        when(config.notifyGamble()).thenReturn(false);
        notifier.onMesBoxNotification("Dragon chainbody! High level gamble count: 100.");
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }
}
