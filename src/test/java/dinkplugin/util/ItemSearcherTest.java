package dinkplugin.util;

import com.google.gson.Gson;
import com.google.inject.testing.fieldbinder.Bind;
import dinkplugin.MockedTestBase;
import net.runelite.api.ItemID;
import net.runelite.http.api.RuneLiteAPI;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;
import org.mockito.Spy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

class ItemSearcherTest extends MockedTestBase {

    @Bind
    private final Gson gson = RuneLiteAPI.GSON;

    @Bind
    private final OkHttpClient httpClient = new OkHttpClient();

    @Spy
    @Bind
    private ItemSearcher itemSearcher;

    @Test
    void test() {
        // Wait for http calls to complete
        verify(itemSearcher, timeout(30_000).atLeastOnce()).populate(any(), any());

        // Ensure correct item mappings are populated
        assertEquals(ItemID.JAR_OF_MIASMA, itemSearcher.findItemId("Jar of miasma"));
        assertEquals(ItemID.UNSIRED, itemSearcher.findItemId("Unsired"));

        assertEquals(ItemID.ALCHEMICAL_HYDRA_HEADS, itemSearcher.findItemId("Alchemical hydra heads"));

        assertEquals(ItemID.AHRIMS_STAFF, itemSearcher.findItemId("Ahrim's staff"));

        assertEquals(ItemID.BRYOPHYTAS_ESSENCE, itemSearcher.findItemId("Bryophyta's essence"));

        assertEquals(ItemID.DRAGON_2H_SWORD, itemSearcher.findItemId("Dragon 2h sword"));
        assertEquals(ItemID.DRAGON_LEGSSKIRT_ORNAMENT_KIT, itemSearcher.findItemId("Dragon legs/skirt ornament kit"));

        assertEquals(ItemID.PET_DAGANNOTH_PRIME, itemSearcher.findItemId("Pet dagannoth prime"));
        assertEquals(ItemID.PET_KRIL_TSUTSAROTH, itemSearcher.findItemId("Pet k'ril tsutsaroth"));
        assertEquals(ItemID.HELLPUPPY, itemSearcher.findItemId("Hellpuppy"));
        assertEquals(ItemID.BABY_MOLE, itemSearcher.findItemId("Baby mole"));
        assertEquals(ItemID.SCORPIAS_OFFSPRING, itemSearcher.findItemId("Scorpia's offspring"));
        assertEquals(ItemID.VETION_JR, itemSearcher.findItemId("Vet'ion jr."));

        assertEquals(ItemID.FEDORA, itemSearcher.findItemId("Fedora"));

        assertEquals(ItemID.SEERCULL, itemSearcher.findItemId("Seercull"));

        assertEquals(ItemID.TZREKJAD, itemSearcher.findItemId("Tzrek-jad"));
        assertEquals(ItemID.JALNIBREK, itemSearcher.findItemId("Jal-nib-rek"));
        assertEquals(ItemID.TOKTZKETXIL, itemSearcher.findItemId("Toktz-ket-xil"));

        assertEquals(ItemID.ENHANCED_CRYSTAL_WEAPON_SEED, itemSearcher.findItemId("Enhanced crystal weapon seed"));

        assertEquals(ItemID.GODSWORD_SHARD_2, itemSearcher.findItemId("Godsword shard 2"));

        assertEquals(ItemID.KQ_HEAD, itemSearcher.findItemId("Kq head"));

        assertEquals(ItemID.TORVA_PLATELEGS_DAMAGED, itemSearcher.findItemId("Torva platelegs (damaged)"));

        assertEquals(ItemID.GIANT_EGG_SACFULL, itemSearcher.findItemId("Giant egg sac(full)"));

        assertEquals(ItemID.TOME_OF_WATER_EMPTY, itemSearcher.findItemId("Tome of water (empty)"));

        assertEquals(ItemID.ZULANDRA_TELEPORT, itemSearcher.findItemId("Zul-andra teleport"));

        assertEquals(ItemID.SCYTHE_OF_VITUR_UNCHARGED, itemSearcher.findItemId("Scythe of vitur (uncharged)"));

        assertEquals(ItemID.ELIDINIS_WARD, itemSearcher.findItemId("Elidinis' ward"));

        assertEquals(ItemID.ICTHLARINS_SHROUD_TIER_2, itemSearcher.findItemId("Icthlarin's shroud (tier 2)"));

        assertEquals(ItemID.BREACH_OF_THE_SCARAB, itemSearcher.findItemId("Breach of the scarab"));

        assertEquals(ItemID.MENAPHITE_ORNAMENT_KIT, itemSearcher.findItemId("Menaphite ornament kit"));

        assertEquals(ItemID.AMULET_OF_DEFENCE_T, itemSearcher.findItemId("Amulet of defence (t)"));

        assertEquals(ItemID.RUNE_SCIMITAR_ORNAMENT_KIT_SARADOMIN, itemSearcher.findItemId("Rune scimitar ornament kit (saradomin)"));

        assertEquals(ItemID.BLACK_PLATEBODY_H2, itemSearcher.findItemId("Black platebody (h2)"));

        assertEquals(ItemID.RING_OF_3RD_AGE, itemSearcher.findItemId("Ring of 3rd age"));

        assertEquals(ItemID.OBSIDIAN_CAPE_R, itemSearcher.findItemId("Obsidian cape (r)"));

        assertEquals(ItemID.CHARGE_DRAGONSTONE_JEWELLERY_SCROLL, itemSearcher.findItemId("Charge dragonstone jewellery scroll"));
    }

}
