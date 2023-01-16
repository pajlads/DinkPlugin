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
import org.jetbrains.annotations.VisibleForTesting;

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

/**
 * This thread-safe singleton holds a mapping of item names to their item id, using the RuneLite API.
 * <p>
 * Unlike {@link net.runelite.client.game.ItemManager#search(String)}, this mapping supports untradable items.
 */
@Slf4j
@Singleton
public class ItemSearcher {
    private final Map<String, Integer> itemIdByName = Collections.synchronizedMap(new HashMap<>(16384));
    private @Inject OkHttpClient httpClient;
    private @Inject Gson gson;

    /**
     * @param name the exact in-game name of an item
     * @return the id associated with the item name, or null if not found
     */
    @Nullable
    public Integer findItemId(@NotNull String name) {
        return itemIdByName.get(name);
    }

    /**
     * Begins the initialization process for {@link #itemIdByName}
     * by querying item names and noted item ids from the RuneLite API,
     * before passing them to {@link #populate(Map, Set)}
     *
     * @implNote This operation does not block the current thread,
     * by utilizing OkHttp's thread pool and Java's Fork-Join common pool.
     */
    @Inject
    void init() {
        queryNamesById().thenAcceptBothAsync(
            queryNotedItemIds().exceptionally(throwable -> Collections.emptySet()),
            this::populate
        );
    }

    /**
     * Populates {@link #itemIdByName} with the inverted mappings of {@code namesById},
     * while skipping noted items specified in {@code notedIds}.
     *
     * @param namesById a mapping of item id's to the corresponding in-game name
     * @param notedIds  the id's of noted items
     * @implNote When multiple non-noted item id's have the same in-game name, only the earliest id is saved
     */
    @VisibleForTesting
    void populate(@NotNull Map<String, String> namesById, @NotNull Set<Integer> notedIds) {
        namesById.forEach((idStr, name) -> {
            int id;
            try {
                id = Integer.parseInt(idStr);
            } catch (NumberFormatException e) {
                log.warn("Failed to parse item id for {}: {}", name, idStr);
                return;
            }

            if (!notedIds.contains(id))
                itemIdByName.putIfAbsent(name, id);
        });

        log.debug("Completed initialization of item cache with {} entries", itemIdByName.size());
    }

    /**
     * @return a mapping of item ids to their in-game names, provided by the RuneLite API
     */
    private CompletableFuture<Map<String, String>> queryNamesById() {
        return queryCache("names.json");
    }

    /**
     * @return a set of id's of noted items, provided by the RuneLite API
     */
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

    /**
     * @param fileName the name of the file to query from RuneLite's cache
     * @return the cache response as a mapping of strings, wrapped in a future
     */
    private CompletableFuture<Map<String, String>> queryCache(@NotNull String fileName) {
        CompletableFuture<Map<String, String>> future = new CompletableFuture<>();

        Request request = new Request.Builder()
            .url(ItemUtils.ITEM_CACHE_BASE_URL + fileName)
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
