package com.horrortricks.peripheralfigure.client.renderer;

import com.horrortricks.peripheralfigure.entity.PeripheralFigureEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;

public class PeripheralFigureRenderer extends LivingEntityRenderer<PeripheralFigureEntity, PlayerModel<PeripheralFigureEntity>> {
   private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("peripheralfigure", "textures/entity/peripheral_figure.png");

   public PeripheralFigureRenderer(Context context) {
      super(context, new PeripheralFigureRenderer.StaticPlayerModel(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
      this.addLayer(new PeripheralFigureRenderer.GlowingEyesLayer(this));
   }

   public ResourceLocation getTextureLocation(PeripheralFigureEntity entity) {
      return TEXTURE;
   }

   public void render(PeripheralFigureEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      int effectiveLight = entity.isLightningSilhouette() ? LightTexture.pack(0, 0) : packedLight;
      super.render(entity, entityYaw, partialTicks, poseStack, buffer, effectiveLight);
   }

   protected void scale(PeripheralFigureEntity entity, PoseStack poseStack, float partialTickTime) {
      poseStack.scale(0.9375F, 0.9375F, 0.9375F);
   }

   protected boolean shouldShowName(PeripheralFigureEntity entity) {
      return false;
   }

   private static final class GlowingEyesLayer extends EyesLayer<PeripheralFigureEntity, PlayerModel<PeripheralFigureEntity>> {
      private static final ResourceLocation EYES_TEXTURE = ResourceLocation.fromNamespaceAndPath("peripheralfigure", "textures/entity/peripheral_figure_eyes.png");
      private static final RenderType EYES_RENDER_TYPE = RenderType.eyes(EYES_TEXTURE);

      GlowingEyesLayer(RenderLayerParent<PeripheralFigureEntity, PlayerModel<PeripheralFigureEntity>> parent) {
         super(parent);
      }

      public RenderType renderType() {
         return EYES_RENDER_TYPE;
      }
   }

   private static final class StaticPlayerModel extends PlayerModel<PeripheralFigureEntity> {
      StaticPlayerModel(ModelPart root) {
         super(root, false);
      }

      public void setupAnim(PeripheralFigureEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
         super.setupAnim(entity, 0.0F, 0.0F, 0.0F, netHeadYaw, headPitch);
      }
   }
}
