package io.github.brainage04.ice_skates;

import io.github.brainage04.ice_skates.item.ModItems;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

import java.util.function.Function;
import java.util.function.Supplier;

public final class IceSkates implements ModInitializer {
    @Override
    public void onInitialize() {
        IceSkatesCommon.initialize();
        ModItems.initialize(IceSkates::registerItem);
    }

    private static Supplier<Item> registerItem(String path, Function<Item.Properties, Item> factory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(IceSkatesCommon.MOD_ID, path));
        Item.Properties properties = new Item.Properties().setId(key);
        Item item = Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties));
        return () -> item;
    }
}
