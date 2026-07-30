package com.horrortricks.peripheralfigure.client;

import com.horrortricks.peripheralfigure.client.renderer.PeripheralFigureRenderer;
import com.horrortricks.peripheralfigure.registry.PeripheralFigureEntities;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers;

@EventBusSubscriber(modid = "peripheralfigure", bus = Bus.MOD, value = Dist.CLIENT)
public final class PeripheralFigureClientEvents {
   private PeripheralFigureClientEvents() {
   }

   @SubscribeEvent
   public static void onRegisterRenderers(RegisterRenderers event) {
      event.registerEntityRenderer((EntityType)PeripheralFigureEntities.PERIPHERAL_FIGURE.get(), PeripheralFigureRenderer::new);
   }
}
