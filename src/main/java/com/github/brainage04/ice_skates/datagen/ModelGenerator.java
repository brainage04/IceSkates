package com.github.brainage04.ice_skates.datagen;

import com.github.brainage04.ice_skates.item.ModItems;
import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.ModelTemplates;

public class ModelGenerator extends FabricModelProvider {
    public ModelGenerator(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockModelGenerators blockModelGenerators) {

    }

    @Override
    public void generateItemModels(ItemModelGenerators itemModelGenerators) {
        itemModelGenerators.generateFlatItem(ModItems.ICE_SKATES, ModelTemplates.FLAT_ITEM);
        itemModelGenerators.generateFlatItem(ModItems.ICE_SKATE_BLADES, ModelTemplates.FLAT_ITEM);
        itemModelGenerators.generateFlatItem(ModItems.ROLLER_SKATES, ModelTemplates.FLAT_ITEM);
        itemModelGenerators.generateFlatItem(ModItems.ROLLER_SKATE_WHEELS, ModelTemplates.FLAT_ITEM);

        itemModelGenerators.generateFlatItem(ModItems.ICE_SWORD, ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModelGenerators.generateFlatItem(ModItems.PACKED_ICE_SWORD, ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModelGenerators.generateFlatItem(ModItems.BLUE_ICE_SWORD, ModelTemplates.FLAT_HANDHELD_ITEM);

    }
}
