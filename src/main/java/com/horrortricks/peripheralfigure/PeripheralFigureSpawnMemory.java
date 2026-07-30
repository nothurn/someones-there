package com.horrortricks.peripheralfigure;

import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

final class PeripheralFigureSpawnMemory {
   private static final String ROOT_KEY = "peripheralfigure_spawn_memory";
   private static final String LAST_X = "LastX";
   private static final String LAST_Y = "LastY";
   private static final String LAST_Z = "LastZ";
   private static final String LAST_RIGHT_SIDE = "LastRightSide";

   private PeripheralFigureSpawnMemory() {
   }

   @Nullable
   static Vec3 getLastSpawnPos(ServerPlayer player) {
      CompoundTag root = player.getPersistentData();
      if (!root.contains("peripheralfigure_spawn_memory")) {
         return null;
      }

      CompoundTag data = root.getCompound("peripheralfigure_spawn_memory");
      return new Vec3(data.getDouble("LastX"), data.getDouble("LastY"), data.getDouble("LastZ"));
   }

   @Nullable
   static Boolean getLastSpawnRightSide(ServerPlayer player) {
      CompoundTag root = player.getPersistentData();
      return !root.contains("peripheralfigure_spawn_memory") ? null : root.getCompound("peripheralfigure_spawn_memory").getBoolean("LastRightSide");
   }

   static void setLastSpawn(ServerPlayer player, Vec3 pos, boolean rightSide) {
      CompoundTag data = new CompoundTag();
      data.putDouble("LastX", pos.x);
      data.putDouble("LastY", pos.y);
      data.putDouble("LastZ", pos.z);
      data.putBoolean("LastRightSide", rightSide);
      player.getPersistentData().put("peripheralfigure_spawn_memory", data);
   }
}
