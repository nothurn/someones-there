package com.horrortricks.peripheralfigure;

import com.horrortricks.peripheralfigure.registry.PeripheralFigureEntities;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig.Type;
import org.slf4j.Logger;

@Mod("peripheralfigure")
public class PeripheralFigureMod {
   public static final String MOD_ID = "peripheralfigure";
   private static final Logger LOGGER = LogUtils.getLogger();

   public PeripheralFigureMod(IEventBus modEventBus, ModContainer modContainer) {
      modContainer.registerConfig(Type.COMMON, PeripheralFigureConfig.SPEC);
      PeripheralFigureEntities.ENTITY_TYPES.register(modEventBus);
      LOGGER.info("Peripheral Figure initialized.");
   }
}
