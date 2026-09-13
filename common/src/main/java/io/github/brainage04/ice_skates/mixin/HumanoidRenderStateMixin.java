package io.github.brainage04.ice_skates.mixin;

import io.github.brainage04.ice_skates.client.SkateRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(HumanoidRenderState.class)
public class HumanoidRenderStateMixin implements SkateRenderState {
    @Unique
    private final ItemStackRenderState ice_skates$boot = new ItemStackRenderState();
    @Unique
    private ArmorTrim ice_skates$trim;
    @Unique
    private ResourceKey<EquipmentAsset> ice_skates$asset;
    @Unique
    private Identifier ice_skates$trimSprite;

    @Override
    public ItemStackRenderState ice_skates$boot() {
        return ice_skates$boot;
    }

    @Override
    public Identifier ice_skates$trimSprite(ArmorTrim trim, ResourceKey<EquipmentAsset> asset) {
        if (!trim.equals(ice_skates$trim) || !asset.equals(ice_skates$asset)) {
            ice_skates$trim = trim;
            ice_skates$asset = asset;
            ice_skates$trimSprite = trim.layerAssetId(EquipmentClientInfo.LayerType.HUMANOID.trimAssetPrefix(), asset);
        }
        return ice_skates$trimSprite;
    }
}
