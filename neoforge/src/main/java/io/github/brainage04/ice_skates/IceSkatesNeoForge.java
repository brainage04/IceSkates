package io.github.brainage04.ice_skates;

import io.github.brainage04.ice_skates.item.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Function;
import java.util.function.Supplier;

@Mod(IceSkatesCommon.MOD_ID)
public final class IceSkatesNeoForge {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(IceSkatesCommon.MOD_ID);

    public IceSkatesNeoForge(IEventBus modBus) {
        IceSkatesCommon.initialize();
        ModItems.initialize(IceSkatesNeoForge::registerItem);
        ITEMS.register(modBus);
        modBus.addListener(this::addCreativeItems);
    }

    private static Supplier<Item> registerItem(String path, Function<Item.Properties, Item> factory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(IceSkatesCommon.MOD_ID, path));
        return ITEMS.register(path, () -> factory.apply(new Item.Properties().setId(key)));
    }

    private void addCreativeItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.COMBAT)) {
            event.accept(ModItems.iceSkates()); event.accept(ModItems.rollerSkates()); event.accept(ModItems.iceSword()); event.accept(ModItems.packedIceSword()); event.accept(ModItems.blueIceSword());
        } else if (event.getTabKey().equals(CreativeModeTabs.INGREDIENTS)) {
            event.accept(ModItems.iceSkateBlades()); event.accept(ModItems.rollerSkateWheels());
        }
    }
}
