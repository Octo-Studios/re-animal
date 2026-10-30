package it.hurts.shatterbyte.reanimal.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import it.hurts.shatterbyte.reanimal.client.light.ColoredGlow;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.UvMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(SubmitNodeCollection.class)
public abstract class SubmitNodeCollectionMixin {
    @ModifyVariable(method = "submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/UvMapping;I)V", at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private int reanimal$tintGlowLight(int tinted, Model<?> model, Object state, PoseStack poseStack, RenderType renderType, int light, int overlay, int unused, UvMapping uvMapping, int outline) {
        return ColoredGlow.apply(tinted, ColoredGlow.tintOf(state, light, poseStack.last()));
    }
}
