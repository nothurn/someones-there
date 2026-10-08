package com.horrortricks.peripheralfigure.client;

import com.horrortricks.peripheralfigure.client.renderer.PeripheralFigureRenderer;
import com.horrortricks.peripheralfigure.registry.PeripheralFigureEntities;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers;

public final class PeripheralFigureClientEvents {
   private PeripheralFigureClientEvents() {
   }

   public static void onRegisterRenderers(RegisterRenderers event) {
      event.registerEntityRenderer((EntityType)PeripheralFigureEntities.PERIPHERAL_FIGURE.get(), PeripheralFigureRenderer::new);
   }
}
