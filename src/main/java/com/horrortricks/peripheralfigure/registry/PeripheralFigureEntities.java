package com.horrortricks.peripheralfigure.registry;

import com.horrortricks.peripheralfigure.entity.PeripheralFigureEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType.Builder;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class PeripheralFigureEntities {
   public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, "peripheralfigure");
   public static final DeferredHolder<EntityType<?>, EntityType<PeripheralFigureEntity>> PERIPHERAL_FIGURE = ENTITY_TYPES.register(
      "peripheral_figure",
      () -> Builder.of(PeripheralFigureEntity::new, MobCategory.MISC).sized(0.6F, 1.95F).fireImmune().clientTrackingRange(12).build("peripheral_figure")
   );

   private PeripheralFigureEntities() {
   }
}
