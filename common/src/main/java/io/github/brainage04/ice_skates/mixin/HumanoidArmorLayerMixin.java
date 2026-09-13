package io.github.brainage04.ice_skates.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.brainage04.ice_skates.client.SkateRenderState;
import io.github.brainage04.ice_skates.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.AtlasIds;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerMixin<S extends HumanoidRenderState> {
    @Unique
    private static final ModelPart ice_skates$trimModel = ice_skates$createTrimModel();

    @Shadow
    protected abstract HumanoidModel<S> getArmorModel(S state, EquipmentSlot slot);

    @Inject(method = "renderArmorPiece", at = @At("HEAD"), cancellable = true)
    private void ice_skates$renderBoots(PoseStack poses, SubmitNodeCollector collector, ItemStack stack,
                                      EquipmentSlot slot, int light, S state, CallbackInfo ci) {
        if (slot != EquipmentSlot.FEET || (!stack.is(ModItems.iceSkates()) && !stack.is(ModItems.rollerSkates()))
                || !HumanoidArmorLayer.shouldRender(stack, slot)) {
            return;
        }

        SkateRenderState skateState = (SkateRenderState) state;
        ItemStackRenderState boot = skateState.ice_skates$boot();
        // NONE selects one unposed boot; every normal item context still displays the approved pair.
        Minecraft.getInstance().getItemModelResolver().updateForTopItem(
                boot, stack, ItemDisplayContext.NONE, null, null, 0);
        ArmorTrim trim = stack.get(DataComponents.TRIM);
        TextureAtlasSprite trimSprite = trim == null ? null : Minecraft.getInstance().getAtlasManager()
                .getAtlasOrThrow(AtlasIds.ARMOR_TRIMS)
                .getSprite(skateState.ice_skates$trimSprite(trim, stack.get(DataComponents.EQUIPPABLE).assetId().orElseThrow()));
        boolean decal = trim != null && trim.pattern().value().decal();
        HumanoidModel<S> model = getArmorModel(state, slot);
        model.setupAnim(state);
        poses.pushPose();
        model.root().translateAndRotate(poses);
        float soleTop = stack.is(ModItems.rollerSkates()) ? 4.25F : 3.4F;
        ice_skates$submitBoot(poses, collector, model.leftLeg, boot, soleTop, light, state.outlineColor, trimSprite, decal);
        ice_skates$submitBoot(poses, collector, model.rightLeg, boot, soleTop, light, state.outlineColor, trimSprite, decal);
        poses.popPose();
        ci.cancel();
    }

    @Unique
    private void ice_skates$submitBoot(PoseStack poses, SubmitNodeCollector collector, ModelPart leg,
                                      ItemStackRenderState boot, float soleTop, int light, int outlineColor,
                                      TextureAtlasSprite trimSprite, boolean decal) {
        poses.pushPose();
        leg.translateAndRotate(poses);
        poses.pushPose();
        // The upper encloses the foot. The runner/wheels sit below it rather than filling its air gaps.
        poses.translate(0, (12 + soleTop - 8) / 16.0F, -3 / 16.0F);
        poses.scale(-1, -1, 1);
        boot.submit(poses, collector, light, OverlayTexture.NO_OVERLAY, outlineColor);
        poses.popPose();
        if (trimSprite != null) {
            // Preserve the actual smithing-template pattern, not just its material color.
            // Standard leg UVs wrap the new 5 x 8.4 x 5 heel/cuff panels.
            poses.translate(0, 3.6F / 16, 0);
            poses.scale(1.255F, .7F, 1.255F);
            collector.submitModelPart(ice_skates$trimModel, poses, Sheets.armorTrimsSheet(decal),
                    light, OverlayTexture.NO_OVERLAY, trimSprite, -1, null, outlineColor);
        }
        poses.popPose();
    }

    @Unique
    private static ModelPart ice_skates$createTrimModel() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("trim", CubeListBuilder.create().texOffs(0, 16)
                .addBox(-2, 0, -2, 4, 12, 4), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32).bakeRoot();
    }
}
