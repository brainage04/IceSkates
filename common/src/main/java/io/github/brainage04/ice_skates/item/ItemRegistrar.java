package io.github.brainage04.ice_skates.item;

import net.minecraft.world.item.Item;

import java.util.function.Function;
import java.util.function.Supplier;

@FunctionalInterface
public interface ItemRegistrar {
    Supplier<Item> register(String path, Function<Item.Properties, Item> factory);
}
