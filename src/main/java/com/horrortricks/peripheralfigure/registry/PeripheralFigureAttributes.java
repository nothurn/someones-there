package com.horrortricks.peripheralfigure.registry;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

@EventBusSubscriber(modid = "peripheralfigure", bus = Bus.MOD)
public final class PeripheralFigureAttributes {
   private PeripheralFigureAttributes() {
   }

   @SubscribeEvent
   public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
      event.put((EntityType)PeripheralFigureEntities.PERIPHERAL_FIGURE.get(), LivingEntity.createLivingAttributes().build());
   }
}
