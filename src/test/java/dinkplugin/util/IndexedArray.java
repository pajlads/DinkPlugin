package dinkplugin.util;

import com.google.common.collect.Iterators;
import lombok.RequiredArgsConstructor;
import net.runelite.api.IndexedObjectSet;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Spliterator;

@RequiredArgsConstructor
public final class IndexedArray<T> implements IndexedObjectSet<T> {
    private final T[] array;

    @Override
    public T byIndex(int index) {
        return array[index];
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        return Iterators.forArray(array);
    }

    @Override
    public Spliterator<T> spliterator() {
        return Arrays.spliterator(array);
    }
}
