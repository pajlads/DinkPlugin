package dinkplugin.util;

import com.google.gson.Gson;
import net.runelite.client.game.ItemManager;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class RarityService extends AbstractRarityService {
    @Inject
    RarityService(Gson gson, ItemManager itemManager) {
        super("/npc_drops.json", 1024, gson, itemManager);
    }
}
