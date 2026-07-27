package io.github.brainage04.ice_skates.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorType;

import java.util.Objects;
import java.util.function.Supplier;

public final class ModItems {
    private static Supplier<Item> iceSkates;
    private static Supplier<Item> iceSkateBlades;
    private static Supplier<Item> rollerSkates;
    private static Supplier<Item> rollerSkateWheels;
    private static Supplier<Item> iceSword;
    private static Supplier<Item> packedIceSword;
    private static Supplier<Item> blueIceSword;

    private ModItems() { }

    public static void initialize(ItemRegistrar registrar) {
        if (iceSkates != null) return;
        iceSkates = registrar.register("ice_skates", properties -> new Item(properties.humanoidArmor(ModArmorMaterials.ICE_SKATES, ArmorType.BOOTS)));
        iceSkateBlades = registrar.register("ice_skate_blades", Item::new);
        rollerSkates = registrar.register("roller_skates", properties -> new Item(properties.humanoidArmor(ModArmorMaterials.ROLLER_SKATES, ArmorType.BOOTS)));
        rollerSkateWheels = registrar.register("roller_skate_wheels", Item::new);
        iceSword = registrar.register("ice_sword", properties -> new Item(properties.sword(ModToolMaterials.ICE, 3.0F, -2.4F)));
        packedIceSword = registrar.register("packed_ice_sword", properties -> new Item(properties.sword(ModToolMaterials.PACKED_ICE, 3.0F, -2.4F)));
        blueIceSword = registrar.register("blue_ice_sword", properties -> new Item(properties.sword(ModToolMaterials.BLUE_ICE, 3.0F, -2.4F)));
    }

    public static Item iceSkates() { return get(iceSkates); }
    public static Item iceSkateBlades() { return get(iceSkateBlades); }
    public static Item rollerSkates() { return get(rollerSkates); }
    public static Item rollerSkateWheels() { return get(rollerSkateWheels); }
    public static Item iceSword() { return get(iceSword); }
    public static Item packedIceSword() { return get(packedIceSword); }
    public static Item blueIceSword() { return get(blueIceSword); }

    private static Item get(Supplier<Item> item) { return Objects.requireNonNull(item, "Items have not been registered").get(); }
}
