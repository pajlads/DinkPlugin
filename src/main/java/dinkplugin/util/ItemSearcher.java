package dinkplugin.util;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class ItemSearcher {

    private static final String RUNELITE_ITEM_CACHE = "https://static.runelite.net/cache/item/";
    private final Map<String, Integer> itemIdByName = Collections.synchronizedMap(new HashMap<>());

    @Inject
    private OkHttpClient httpClient;

    @Inject
    private Gson gson;

    @Nullable
    public Integer findItemId(String name) {
        return itemIdByName.get(name);
    }

    @Nullable
    public String getItemImageUrl(String itemName) {
        Integer itemId = findItemId(itemName);
        return itemId != null ? ItemUtils.getItemImageUrl(itemId) : null;
    }

    @Inject
    void init() {
        queryNamesById().thenAcceptBothAsync(
            queryNotedItemIds().exceptionally(throwable -> Collections.emptySet()),
            this::merge
        );
    }

    private void merge(Map<String, String> namesById, Set<Integer> notedIds) {
        namesById.forEach((idStr, name) -> {
            int id;
            try {
                id = Integer.parseInt(idStr);
            } catch (NumberFormatException e) {
                log.warn("Failed to parse item id for {}: {}", name, idStr);
                return;
            }

            if (!notedIds.contains(id))
                itemIdByName.put(name, id);
        });

        log.debug("Completed initialization of item cache with {} entries", itemIdByName.size());
    }

    private CompletableFuture<Map<String, String>> queryNamesById() {
        return queryCache("names.json");
    }

    private CompletableFuture<Set<Integer>> queryNotedItemIds() {
        return queryCache("notes.json").thenApply(items ->
            items.keySet().stream()
                .map(id -> {
                    try {
                        return Integer.parseInt(id);
                    } catch (NumberFormatException e) {
                        log.warn("Failed to parse noted item id: {}", id);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet())
        );
    }

    private CompletableFuture<Map<String, String>> queryCache(@NotNull String fileName) {
        CompletableFuture<Map<String, String>> future = new CompletableFuture<>();

        Request request = new Request.Builder()
            .url(RUNELITE_ITEM_CACHE + fileName)
            .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                log.error("Failed to query " + fileName, e);
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                assert response.body() != null;
                try (Reader reader = response.body().charStream()) {
                    Type type = new TypeToken<Map<String, String>>() {}.getType();
                    Map<String, String> items = gson.fromJson(reader, type);
                    future.complete(items);
                } catch (JsonParseException e) {
                    log.error("Failed to parse " + fileName, e);
                    future.completeExceptionally(e);
                } finally {
                    response.close();
                }
            }
        });

        return future;
    }

}
