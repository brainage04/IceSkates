package io.github.brainage04.ice_skates.item;

import io.github.brainage04.ice_skates.IceSkates;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorType;

import java.util.function.Function;

public class ModItems {
    public static final Item ICE_SKATES = register("ice_skates", Item::new, new Item.Properties().humanoidArmor(ModArmorMaterials.ICE_SKATES, ArmorType.BOOTS));
    public static final Item ICE_SKATE_BLADES = register("ice_skate_blades", Item::new, new Item.Properties());
    public static final Item ROLLER_SKATES = register("roller_skates", Item::new, new Item.Properties().humanoidArmor(ModArmorMaterials.ROLLER_SKATES, ArmorType.BOOTS));
    public static final Item ROLLER_SKATE_WHEELS = register("roller_skate_wheels", Item::new, new Item.Properties());

    public static final Item ICE_SWORD = register("ice_sword", Item::new, new Item.Properties().sword(ModToolMaterials.ICE, 3.0F, -2.4F));
    public static final Item PACKED_ICE_SWORD = register("packed_ice_sword", Item::new, new Item.Properties().sword(ModToolMaterials.PACKED_ICE, 3.0F, -2.4F));
    public static final Item BLUE_ICE_SWORD = register("blue_ice_sword", Item::new, new Item.Properties().sword(ModToolMaterials.BLUE_ICE, 3.0F, -2.4F));

    public static Item register(String path, Function<Item.Properties, Item> factory, Item.Properties settings) {
        final ResourceKey<Item> registryKey = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(IceSkates.MOD_ID, path));
        settings.setId(registryKey);
        return Registry.register(BuiltInRegistries.ITEM, registryKey, factory.apply(settings));
    }

    public static void initialize() {
        // Class loading registers all items.
    }
}
