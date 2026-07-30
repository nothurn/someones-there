package com.horrortricks.peripheralfigure;

import net.minecraft.server.level.ServerPlayer;

final class PeripheralFigureAloneCheck {
   private PeripheralFigureAloneCheck() {
   }

   static boolean isAlone(ServerPlayer player) {
      return player.getServer().getPlayerList().getPlayerCount() <= 1;
   }
}
