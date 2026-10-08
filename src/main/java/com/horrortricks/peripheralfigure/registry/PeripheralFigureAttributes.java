package com.horrortricks.peripheralfigure.registry;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

public final class PeripheralFigureAttributes {
   private PeripheralFigureAttributes() {
   }

   public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
      event.put((EntityType)PeripheralFigureEntities.PERIPHERAL_FIGURE.get(), LivingEntity.createLivingAttributes().build());
   }
}
