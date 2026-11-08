package io.github.brainage04.ice_skates.mixin;

import io.github.brainage04.ice_skates.client.SkateRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.client.resources.palette.PalettedTextureManager;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(HumanoidRenderState.class)
public class HumanoidRenderStateMixin implements SkateRenderState {
    @Unique private final ItemStackRenderState ice_skates$boot = new ItemStackRenderState();
    @Unique private ArmorTrim ice_skates$trim;
    @Unique private Identifier ice_skates$trimTexture;

    @Override
    public ItemStackRenderState ice_skates$boot() {
        return ice_skates$boot;
    }

    @Override
    public PalettedTextureManager.Handle ice_skates$trimTexture(ArmorTrim trim) {
        if (!trim.equals(ice_skates$trim)) {
            ice_skates$trim = trim;
            // The same pattern texture vanilla's humanoid armor layer uses. The skates' equipment
            // asset defines no
            // trim overrides, so the trim material's own palette always applies.
            String prefix = EquipmentClientInfo.LayerType.HUMANOID.trimAssetPrefix();
            ice_skates$trimTexture =
                    trim.pattern().value().assetId().withPath(path -> prefix + "/" + path);
        }
        // The manager caches one handle per texture and palette, so this is a lookup after the
        // first frame.
        return Minecraft.getInstance()
                .getPalettedTextureManager()
                .getOrPrepare(ice_skates$trimTexture, trim.material().value().paletteId());
    }
}
