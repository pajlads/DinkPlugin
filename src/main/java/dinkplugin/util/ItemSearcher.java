package dinkplugin.util;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class ItemSearcher {

    private static final String RUNELITE_ITEM_CACHE = "https://static.runelite.net/cache/item/";
    @Inject
    private OkHttpClient okHttpClient;
    @Inject
    Gson gson;
    private Map<String, Integer> nameAndIds = new HashMap<>();

    @Inject
    public void constructor() {
        loadItemIdsAndNames();
    }

    public void loadItemIdsAndNames() {
        Request request = new Request.Builder().url(RUNELITE_ITEM_CACHE + "names.json").build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            try {
                assert response.body() != null;
                Type type = new TypeToken<Map<String, String>>() {
                }.getType();
                Map<String, String> items = gson.fromJson(response.body().charStream(), type);

                filterNotedItems(items);
            } catch (JsonSyntaxException | JsonIOException e) {
                log.error("Failed to load names");
            }
        } catch (IOException e) {
            log.warn("Failed to load item names and ids.");
        }
    }

    private void filterNotedItems(Map<String, String> items) {
        Request request = new Request.Builder().url(RUNELITE_ITEM_CACHE + "notes.json").build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            try {
                assert response.body() != null;
                Type type = new TypeToken<Map<String, String>>() {
                }.getType();
                Map<String, String> notes = gson.fromJson(response.body().charStream(), type);
                notes.keySet().forEach(items::remove);
            } catch (JsonSyntaxException | JsonIOException e) {
                log.error("Failed to filter noted items");
            }
            // Always set the items even when failing to filter noted items.
            nameAndIds = items
                .entrySet()
                .stream()
                .collect(
                    Collectors.toMap(Map.Entry::getValue, (e) -> Integer.parseInt(e.getKey()), (Integer a, Integer b) -> a)
                );

        } catch (IOException e) {
            log.warn("Failed to remove noted items.");
        }
    }

    public String getItemImageUrl(String itemName) {
        Integer itemId = findItemId(itemName);
        if (itemId != null) {
            return ItemUtils.getItemImageUrl(itemId);
        }

        return null;
    }

    public Integer findItemId(String name) {
        return nameAndIds.get(name);
    }
}
