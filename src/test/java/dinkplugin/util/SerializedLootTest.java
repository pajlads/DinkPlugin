package dinkplugin.util;

import com.google.gson.Gson;
import net.runelite.api.ItemID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SerializedLootTest {

    @Test
    void deserialize() {
        String json = "{\"type\":\"NPC\",\"name\":\"Bryophyta\",\"kills\":16,\"first\":1708910620551,\"last\":1708983457752,\"drops\":[23182,16,532,16,1618,5,1620,5,2363,2,560,100,1079,2,890,100,1303,1,1113,2,1147,2,562,200,1124,5,1289,4,563,200]}";
        SerializedLoot lootRecord = new Gson().fromJson(json, SerializedLoot.class);
        assertEquals(16, lootRecord.getKills());
        assertEquals(2, lootRecord.getQuantity(ItemID.RUNE_CHAINBODY));
    }

}
