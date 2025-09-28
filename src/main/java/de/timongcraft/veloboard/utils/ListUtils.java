package de.timongcraft.veloboard.utils;

import org.jetbrains.annotations.ApiStatus;

import java.util.List;
import java.util.function.Supplier;

@ApiStatus.Internal
public class ListUtils {

    public static <T> void setOrPad(List<T> list, int index, T element, Supplier<T> filler) {
        if (index < list.size()) {
            list.set(index, element);
            return;
        }

        while (list.size() < index) {
            list.add(filler.get());
        }

        list.add(element);
    }

    private ListUtils() {}

}