package dinkplugin.util;

import com.google.gson.Gson;
import com.google.inject.testing.fieldbinder.Bind;
import dinkplugin.MockedTestBase;
import net.runelite.api.gameval.ItemID;
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
        assertEquals(ItemID.ABYSSALSIRE_UNSIRED, itemSearcher.findItemId("Unsired"));

        assertEquals(ItemID.POH_ALCHEMICAL_HYDRA_HEAD, itemSearcher.findItemId("Alchemical hydra heads"));

        assertEquals(ItemID.BARROWS_AHRIM_WEAPON, itemSearcher.findItemId("Ahrim's staff"));

        assertEquals(ItemID.GB_MOSS_ESSENCE, itemSearcher.findItemId("Bryophyta's essence"));

        assertEquals(ItemID.DRAGON_2H_SWORD, itemSearcher.findItemId("Dragon 2h sword"));
        assertEquals(ItemID.DRAGON_PLATELEGS_KIT, itemSearcher.findItemId("Dragon legs/skirt ornament kit"));

        assertEquals(ItemID.PRIMEPET, itemSearcher.findItemId("Pet dagannoth prime"));
        assertEquals(ItemID.ZAMORAKPET, itemSearcher.findItemId("Pet k'ril tsutsaroth"));
        assertEquals(ItemID.HELL_PET, itemSearcher.findItemId("Hellpuppy"));
        assertEquals(ItemID.MOLEPET, itemSearcher.findItemId("Baby mole"));
        assertEquals(ItemID.SCORPIA_PET, itemSearcher.findItemId("Scorpia's offspring"));
        assertEquals(ItemID.VETION_PET, itemSearcher.findItemId("Vet'ion jr."));

        assertEquals(ItemID.FEDORA, itemSearcher.findItemId("Fedora"));

        assertEquals(ItemID.DAGANOTH_CAVE_MAGIC_SHORTBOW, itemSearcher.findItemId("Seercull"));

        assertEquals(ItemID.JAD_PET, itemSearcher.findItemId("Tzrek-jad"));
        assertEquals(ItemID.INFERNOPET, itemSearcher.findItemId("Jal-nib-rek"));
        assertEquals(ItemID.TZHAAR_SPIKESHIELD, itemSearcher.findItemId("Toktz-ket-xil"));

        assertEquals(ItemID.PRIF_WEAPON_SEED_ENHANCED, itemSearcher.findItemId("Enhanced crystal weapon seed"));

        assertEquals(ItemID.GODWARS_GODSWORD_BLADE2, itemSearcher.findItemId("Godsword shard 2"));

        assertEquals(ItemID.POH_TROPHYDROP_KALPHITEQUEEN, itemSearcher.findItemId("Kq head"));

        assertEquals(ItemID.BROKEN_TORVA_LEGS, itemSearcher.findItemId("Torva platelegs (damaged)"));

        assertEquals(ItemID.HOSDUN_EGG_SAC_FULL, itemSearcher.findItemId("Giant egg sac(full)"));

        assertEquals(ItemID.TOME_OF_WATER_UNCHARGED, itemSearcher.findItemId("Tome of water (empty)"));

        assertEquals(ItemID.TELEPORTSCROLL_ZULANDRA, itemSearcher.findItemId("Zul-andra teleport"));

        assertEquals(ItemID.SCYTHE_OF_VITUR_UNCHARGED, itemSearcher.findItemId("Scythe of vitur (uncharged)"));

        assertEquals(ItemID.ELIDINIS_WARD, itemSearcher.findItemId("Elidinis' ward"));

        assertEquals(ItemID.ICTHLARINS_SHROUD_2, itemSearcher.findItemId("Icthlarin's shroud (tier 2)"));

        assertEquals(ItemID.BREACH_OF_THE_SCARAB, itemSearcher.findItemId("Breach of the scarab"));

        assertEquals(ItemID.ELIDINIS_WARD_ORNAMENT_KIT, itemSearcher.findItemId("Menaphite ornament kit"));

        assertEquals(ItemID.AMULET_OF_DEFENCE_T, itemSearcher.findItemId("Amulet of defence (t)"));

        assertEquals(ItemID.RUNE_SCIMITAR_ORNAMENT_KIT_SARADOMIN, itemSearcher.findItemId("Rune scimitar ornament kit (saradomin)"));

        assertEquals(ItemID.BLACK_PLATEBODY_H2, itemSearcher.findItemId("Black platebody (h2)"));

        assertEquals(ItemID.RING_OF_3RD_AGE, itemSearcher.findItemId("Ring of 3rd age"));

        assertEquals(ItemID.TZHAAR_CAPE_OBSIDIAN_R, itemSearcher.findItemId("Obsidian cape (r)"));

        assertEquals(ItemID.SCROLL_CHARGE_DRAGONSTONE, itemSearcher.findItemId("Charge dragonstone jewellery scroll"));
    }

}
