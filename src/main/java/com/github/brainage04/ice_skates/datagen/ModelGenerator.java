package com.github.brainage04.ice_skates.datagen;

import com.github.brainage04.ice_skates.item.ModEquipmentAssets;
import com.github.brainage04.ice_skates.item.ModItems;
import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.client.color.item.Dye;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.SelectItemModel;
import net.minecraft.client.renderer.item.properties.select.TrimMaterialProperty;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.trim.TrimMaterial;

import java.util.ArrayList;
import java.util.List;

import static net.minecraft.client.data.models.ItemModelGenerators.TRIM_MATERIAL_MODELS;

public class ModelGenerator extends FabricModelProvider {
    public ModelGenerator(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockModelGenerators blockModelGenerators) {

    }

    @Override
    public void generateItemModels(ItemModelGenerators itemModelGenerators) {
        itemModelGenerators.generateFlatItem(ModItems.ICE_SKATE_BLADES, ModelTemplates.FLAT_ITEM);
        itemModelGenerators.generateFlatItem(ModItems.ROLLER_SKATE_WHEELS, ModelTemplates.FLAT_ITEM);

        generateTrimmableItem(itemModelGenerators, ModItems.ICE_SKATES, ModEquipmentAssets.ICE_SKATES, ItemModelGenerators.TRIM_PREFIX_BOOTS, true, 1644825);
        generateTrimmableItem(itemModelGenerators, ModItems.ROLLER_SKATES, ModEquipmentAssets.ROLLER_SKATES, ItemModelGenerators.TRIM_PREFIX_BOOTS, true, 15132390);

        itemModelGenerators.generateFlatItem(ModItems.ICE_SWORD, ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModelGenerators.generateFlatItem(ModItems.PACKED_ICE_SWORD, ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModelGenerators.generateFlatItem(ModItems.BLUE_ICE_SWORD, ModelTemplates.FLAT_HANDHELD_ITEM);
    }

    public final void generateTrimmableItem(ItemModelGenerators itemModelGenerators, Item item, ResourceKey<EquipmentAsset> resourceKey, Identifier identifier, boolean dyeable, int defaultColour) {
        Identifier identifier2 = ModelLocationUtils.getModelLocation(item);
        Identifier identifier3 = TextureMapping.getItemTexture(item);
        Identifier identifier4 = TextureMapping.getItemTexture(item, "_overlay");
        List<SelectItemModel.SwitchCase<ResourceKey<TrimMaterial>>> list = new ArrayList<>(TRIM_MATERIAL_MODELS.size());

        for (ItemModelGenerators.TrimMaterialData trimMaterialData : TRIM_MATERIAL_MODELS) {
            Identifier identifier5 = identifier2.withSuffix("_" + trimMaterialData.assets().base().suffix() + "_trim");
            Identifier identifier6 = identifier.withSuffix("_" + trimMaterialData.assets().assetId(resourceKey).suffix());
            ItemModel.Unbaked unbaked;
            if (dyeable) {
                itemModelGenerators.generateLayeredItem(identifier5, identifier3, identifier4, identifier6);
                unbaked = ItemModelUtils.tintedModel(identifier5, new Dye(defaultColour));
            } else {
                itemModelGenerators.generateLayeredItem(identifier5, identifier3, identifier6);
                unbaked = ItemModelUtils.plainModel(identifier5);
            }

            list.add(ItemModelUtils.when(trimMaterialData.materialKey(), unbaked));
        }

        ItemModel.Unbaked unbaked2;
        if (dyeable) {
            ModelTemplates.TWO_LAYERED_ITEM.create(identifier2, TextureMapping.layered(identifier3, identifier4), itemModelGenerators.modelOutput);
            unbaked2 = ItemModelUtils.tintedModel(identifier2, new Dye(defaultColour));
        } else {
            ModelTemplates.FLAT_ITEM.create(identifier2, TextureMapping.layer0(identifier3), itemModelGenerators.modelOutput);
            unbaked2 = ItemModelUtils.plainModel(identifier2);
        }

        itemModelGenerators.itemModelOutput.accept(item, ItemModelUtils.select(new TrimMaterialProperty(), unbaked2, list));
    }
}
