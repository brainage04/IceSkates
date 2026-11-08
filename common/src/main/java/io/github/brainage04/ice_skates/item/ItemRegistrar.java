package io.github.brainage04.ice_skates.item;

import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.world.item.Item;

@FunctionalInterface
public interface ItemRegistrar {
    Supplier<Item> register(String path, Function<Item.Properties, Item> factory);
}
