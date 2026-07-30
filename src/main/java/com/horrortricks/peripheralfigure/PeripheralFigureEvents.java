package com.horrortricks.peripheralfigure;

import com.horrortricks.peripheralfigure.entity.PeripheralFigureEntity;
import com.horrortricks.peripheralfigure.registry.PeripheralFigureEntities;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent.Post;
import org.slf4j.Logger;

@EventBusSubscriber(modid = "peripheralfigure")
public final class PeripheralFigureEvents {
   private static final int DEBUG_COMMAND_PERMISSION_LEVEL = 0;
   private static final int MAX_GROUND_SEARCH = 8;
   private static final int CANDIDATE_POOL_SIZE = 5;
   private static final double FIGURE_EYE_HEIGHT = 1.62;
   private static final double OPPOSITE_SIDE_WEIGHT_BONUS = 8.0;
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Map<UUID, Long> NEXT_ALLOWED_SPAWN_GAME_TIME = new HashMap<>();
   private static final Map<UUID, Integer> ACTIVE_FIGURE_ENTITY_ID = new HashMap<>();
   private static final Map<UUID, Long> SCHEDULED_SECOND_APPEARANCE_GAME_TIME = new HashMap<>();
   private static final Map<UUID, Long> NEXT_ALLOWED_LINGER_GAME_TIME = new HashMap<>();

   private PeripheralFigureEvents() {
   }

   @SubscribeEvent
   public static void onLevelTick(Post event) {
      if (event.getLevel() instanceof ServerLevel level && (Boolean)PeripheralFigureConfig.ENABLED.get()) {
         long gameTime = level.getGameTime();

         for (ServerPlayer player : level.players()) {
            UUID id = player.getUUID();
            if (!hasActiveFigure(level, id)) {
               Long scheduledSecond = SCHEDULED_SECOND_APPEARANCE_GAME_TIME.get(id);
               if (scheduledSecond != null) {
                  if (gameTime >= scheduledSecond) {
                     SCHEDULED_SECOND_APPEARANCE_GAME_TIME.remove(id);
                     trySpawn(level, player, player.getRandom(), false, false);
                     NEXT_ALLOWED_SPAWN_GAME_TIME.put(id, gameTime + cooldownTicksFor(player));
                  }
               } else {
                  Long nextAllowed = NEXT_ALLOWED_SPAWN_GAME_TIME.get(id);
                  if (nextAllowed == null || gameTime >= nextAllowed) {
                     double chance = (Double)PeripheralFigureConfig.SPAWN_CHANCE_PER_TICK.get();
                     if (PeripheralFigureAloneCheck.isAlone(player)) {
                        chance *= PeripheralFigureConfig.ALONE_SPAWN_CHANCE_MULTIPLIER.get();
                     }

                     if (!(player.getRandom().nextDouble() >= chance)) {
                        trySpawn(level, player, player.getRandom(), false, false);
                        NEXT_ALLOWED_SPAWN_GAME_TIME.put(id, gameTime + cooldownTicksFor(player));
                     }
                  }
               }
            }
         }
      }
   }

   private static long cooldownTicksFor(ServerPlayer player) {
      long cooldown = (Long)PeripheralFigureConfig.SPAWN_COOLDOWN_TICKS.get();
      if (PeripheralFigureAloneCheck.isAlone(player)) {
         cooldown = Math.round(cooldown * (Double)PeripheralFigureConfig.ALONE_COOLDOWN_MULTIPLIER.get());
      }

      return cooldown;
   }

   @SubscribeEvent
   public static void onPlayerLoggedOut(PlayerLoggedOutEvent event) {
      UUID id = event.getEntity().getUUID();
      NEXT_ALLOWED_SPAWN_GAME_TIME.remove(id);
      ACTIVE_FIGURE_ENTITY_ID.remove(id);
      SCHEDULED_SECOND_APPEARANCE_GAME_TIME.remove(id);
      NEXT_ALLOWED_LINGER_GAME_TIME.remove(id);
   }

   @SubscribeEvent
   public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
      if (event.getEntity() instanceof LightningBolt bolt && event.getLevel() instanceof ServerLevel level) {
         if ((Boolean)PeripheralFigureConfig.ENABLED.get() && (Boolean)PeripheralFigureConfig.LIGHTNING_SILHOUETTE_ENABLED.get()) {
            double detectionRadius = (Double)PeripheralFigureConfig.LIGHTNING_SILHOUETTE_DETECTION_RADIUS.get();

            for (ServerPlayer player : level.players()) {
               if (!hasActiveFigure(level, player.getUUID())
                  && !(player.position().distanceTo(bolt.position()) > detectionRadius)
                  && !(player.getRandom().nextDouble() >= (Double)PeripheralFigureConfig.LIGHTNING_SILHOUETTE_CHANCE.get())) {
                  trySpawnLightningSilhouette(level, player, player.getRandom());
               }
            }
         }
      }
   }

   @SubscribeEvent
   public static void onRegisterCommands(RegisterCommandsEvent event) {
      register(event.getDispatcher());
   }

   private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("peripheralfigure").requires(source -> source.hasPermission(0)))
            .then(
               ((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("debug")
                        .then(Commands.literal("spawn").executes(context -> debugSpawn((CommandSourceStack)context.getSource()))))
                     .then(Commands.literal("lightning").executes(context -> debugLightning((CommandSourceStack)context.getSource()))))
                  .then(Commands.literal("linger").executes(context -> debugLinger((CommandSourceStack)context.getSource())))
            )
      );
   }

   private static int debugSpawn(CommandSourceStack source) {
      if (source.getEntity() instanceof ServerPlayer player) {
         boolean var3 = trySpawn(player.serverLevel(), player, player.getRandom(), true, false);
         LOGGER.info(
            "[peripheralfigure debug] {} for {}.",
            var3
               ? "Spawned figure"
               : "No valid candidate found (every angle/distance rolled was rejected by the ground or last-position check) - no figure spawned",
            player.getGameProfile().getName()
         );
         return var3 ? 1 : 0;
      } else {
         LOGGER.info("[peripheralfigure debug] /peripheralfigure debug spawn must be run by a player.");
         return 0;
      }
   }

   private static int debugLightning(CommandSourceStack source) {
      if (source.getEntity() instanceof ServerPlayer player) {
         boolean var3 = trySpawnLightningSilhouette(player.serverLevel(), player, player.getRandom());
         LOGGER.info(
            "[peripheralfigure debug] {} for {} (bypassing the need for an actual nearby lightning strike).",
            var3 ? "Spawned a lightning silhouette" : "No valid candidate found - no silhouette spawned",
            player.getGameProfile().getName()
         );
         return var3 ? 1 : 0;
      } else {
         LOGGER.info("[peripheralfigure debug] /peripheralfigure debug lightning must be run by a player.");
         return 0;
      }
   }

   private static int debugLinger(CommandSourceStack source) {
      if (source.getEntity() instanceof ServerPlayer player) {
         boolean var3 = trySpawn(player.serverLevel(), player, player.getRandom(), true, true);
         LOGGER.info(
            "[peripheralfigure debug] {} for {} (linger variant forced).",
            var3 ? "Spawned figure" : "No valid candidate found - no figure spawned",
            player.getGameProfile().getName()
         );
         return var3 ? 1 : 0;
      } else {
         LOGGER.info("[peripheralfigure debug] /peripheralfigure debug linger must be run by a player.");
         return 0;
      }
   }

   public static void onTimeoutDespawn(Level level, UUID playerUuid, RandomSource random) {
      if (!(random.nextDouble() >= (Double)PeripheralFigureConfig.SECOND_APPEARANCE_CHANCE.get())) {
         long min = (Long)PeripheralFigureConfig.SECOND_APPEARANCE_DELAY_MIN_TICKS.get();
         long max = Math.max(min, (Long)PeripheralFigureConfig.SECOND_APPEARANCE_DELAY_MAX_TICKS.get());
         long delay = min + random.nextInt((int)(max - min) + 1);
         SCHEDULED_SECOND_APPEARANCE_GAME_TIME.put(playerUuid, level.getGameTime() + delay);
      }
   }

   private static boolean hasActiveFigure(ServerLevel level, UUID playerId) {
      Integer entityId = ACTIVE_FIGURE_ENTITY_ID.get(playerId);
      if (entityId == null) {
         return false;
      }

      Entity entity = level.getEntity(entityId);
      if (entity instanceof PeripheralFigureEntity && entity.isAlive()) {
         return true;
      }

      ACTIVE_FIGURE_ENTITY_ID.remove(playerId);
      return false;
   }

   private static boolean trySpawn(ServerLevel level, ServerPlayer player, RandomSource random, boolean bypassVisibilityChecks, boolean forceLinger) {
      boolean alone = PeripheralFigureAloneCheck.isAlone(player);
      double minDistance = (Double)PeripheralFigureConfig.MIN_DISTANCE.get();
      double maxDistance = (Double)PeripheralFigureConfig.MAX_DISTANCE.get();
      double minDistanceFromLast = (Double)PeripheralFigureConfig.MIN_DISTANCE_FROM_LAST_SPAWN.get();
      if (alone) {
         minDistance *= PeripheralFigureConfig.ALONE_DISTANCE_MULTIPLIER.get();
         maxDistance *= PeripheralFigureConfig.ALONE_DISTANCE_MULTIPLIER.get();
         minDistanceFromLast *= PeripheralFigureConfig.ALONE_DISTANCE_MULTIPLIER.get();
      }

      Vec3 lastSpawnPos = PeripheralFigureSpawnMemory.getLastSpawnPos(player);
      List<PeripheralFigureEvents.Candidate> candidates = new ArrayList<>();

      for (int attempt = 0; attempt < 5; attempt++) {
         double band = (Double)PeripheralFigureConfig.PERIPHERAL_ANGLE_MIN.get()
            + random.nextDouble() * ((Double)PeripheralFigureConfig.PERIPHERAL_ANGLE_MAX.get() - (Double)PeripheralFigureConfig.PERIPHERAL_ANGLE_MIN.get());
         boolean rightSide = random.nextBoolean();
         double angleDegrees = rightSide ? band : -band;
         double distance = minDistance + random.nextDouble() * (maxDistance - minDistance);
         double placementYaw = player.getYRot() + angleDegrees;
         double rad = Math.toRadians(placementYaw);
         double targetX = player.getX() - Math.sin(rad) * distance;
         double targetZ = player.getZ() + Math.cos(rad) * distance;
         BlockPos groundPos = findGroundedPos(level, BlockPos.containing(targetX, player.getY(), targetZ));
         if (groundPos == null) {
            LOGGER.info(
               "[peripheralfigure debug] attempt {}: REJECTED, no standable ground found searching near ({}, ~{}, {})",
               new Object[]{attempt, (int)targetX, player.getY(), (int)targetZ}
            );
         } else if (lastSpawnPos != null && Vec3.atBottomCenterOf(groundPos).distanceTo(lastSpawnPos) < minDistanceFromLast) {
            LOGGER.info(
               "[peripheralfigure debug] attempt {}: REJECTED, {} is within {} blocks of last spawn position",
               new Object[]{attempt, groundPos, minDistanceFromLast}
            );
         } else {
            int lightLevel = level.getMaxLocalRawBrightness(groundPos);
            Vec3 candidateEyePos = new Vec3(groundPos.getX() + 0.5, groundPos.getY() + 1.62, groundPos.getZ() + 0.5);
            boolean losOk = hasLineOfSight(level, player.getEyePosition(), candidateEyePos);
            if (!bypassVisibilityChecks) {
               if (!losOk) {
                  LOGGER.info("[peripheralfigure debug] attempt {}: REJECTED, line of sight to {} is blocked", attempt, groundPos);
                  continue;
               }

               if (lightLevel < (Integer)PeripheralFigureConfig.MIN_LIGHT_LEVEL.get()) {
                  LOGGER.info(
                     "[peripheralfigure debug] attempt {}: REJECTED, light level {} at {} is below min_light_level",
                     new Object[]{attempt, lightLevel, groundPos}
                  );
                  continue;
               }
            }

            LOGGER.info(
               "[peripheralfigure debug] attempt {}: ACCEPTED at {} (light={}, line-of-sight-clear={}{})",
               new Object[]{attempt, groundPos, lightLevel, losOk, bypassVisibilityChecks ? ", visibility checks bypassed for debug" : ""}
            );
            candidates.add(new PeripheralFigureEvents.Candidate(groundPos, rightSide, lightLevel));
         }
      }

      if (candidates.isEmpty()) {
         return false;
      }

      Boolean lastRightSide = PeripheralFigureSpawnMemory.getLastSpawnRightSide(player);
      PeripheralFigureEvents.Candidate chosen = pickWeighted(candidates, random, lastRightSide);
      spawnFigureAt(level, player, chosen.groundPos, chosen.rightSide, forceLinger);
      return true;
   }

   private static PeripheralFigureEvents.Candidate pickWeighted(
      List<PeripheralFigureEvents.Candidate> candidates, RandomSource random, @Nullable Boolean lastRightSide
   ) {
      double[] weights = new double[candidates.size()];
      double total = 0.0;

      for (int i = 0; i < candidates.size(); i++) {
         PeripheralFigureEvents.Candidate candidate = candidates.get(i);
         double weight = 16.0 - candidate.lightLevel();
         if (lastRightSide != null && candidate.rightSide() != lastRightSide) {
            weight += 8.0;
         }

         weights[i] = Math.max(weight, 0.1);
         total += weights[i];
      }

      double roll = random.nextDouble() * total;
      double accumulated = 0.0;

      for (int i = 0; i < candidates.size(); i++) {
         accumulated += weights[i];
         if (roll < accumulated) {
            return candidates.get(i);
         }
      }

      return candidates.get(candidates.size() - 1);
   }

   private static void spawnFigureAt(ServerLevel level, ServerPlayer player, BlockPos groundPos, boolean rightSide, boolean forceLinger) {
      PeripheralFigureEntity figure = new PeripheralFigureEntity(
         (EntityType<? extends PeripheralFigureEntity>)PeripheralFigureEntities.PERIPHERAL_FIGURE.get(), level
      );
      double spawnX = groundPos.getX() + 0.5;
      double spawnZ = groundPos.getZ() + 0.5;
      figure.setPos(spawnX, groundPos.getY(), spawnZ);
      double dx = player.getX() - spawnX;
      double dz = player.getZ() - spawnZ;
      double bearingToPlayer = Math.toDegrees(Math.atan2(-dx, dz));
      float yaw = (float)Mth.wrapDegrees(bearingToPlayer);
      figure.setYRot(yaw);
      figure.setYHeadRot(yaw);
      figure.setYBodyRot(yaw);
      figure.setXRot(0.0F);
      figure.setTargetPlayer(player.getUUID());
      figure.anchorHere();
      if (forceLinger || isLingerEligible(level, player)) {
         figure.markAsLingerVariant();
         NEXT_ALLOWED_LINGER_GAME_TIME.put(player.getUUID(), level.getGameTime() + (Long)PeripheralFigureConfig.LINGER_VARIANT_COOLDOWN_TICKS.get());
      }

      level.addFreshEntity(figure);
      ACTIVE_FIGURE_ENTITY_ID.put(player.getUUID(), figure.getId());
      PeripheralFigureSpawnMemory.setLastSpawn(player, Vec3.atBottomCenterOf(groundPos), rightSide);
   }

   private static boolean isLingerEligible(ServerLevel level, ServerPlayer player) {
      if (!(Boolean)PeripheralFigureConfig.LINGER_VARIANT_ENABLED.get()) {
         return false;
      }

      Long nextAllowed = NEXT_ALLOWED_LINGER_GAME_TIME.get(player.getUUID());
      return nextAllowed != null && level.getGameTime() < nextAllowed
         ? false
         : player.getRandom().nextDouble() < (Double)PeripheralFigureConfig.LINGER_VARIANT_CHANCE.get();
   }

   private static boolean trySpawnLightningSilhouette(ServerLevel level, ServerPlayer player, RandomSource random) {
      double minDistance = (Double)PeripheralFigureConfig.LIGHTNING_SILHOUETTE_MIN_DISTANCE.get();
      double maxDistance = (Double)PeripheralFigureConfig.LIGHTNING_SILHOUETTE_MAX_DISTANCE.get();
      double maxViewAngle = (Double)PeripheralFigureConfig.LIGHTNING_SILHOUETTE_VIEW_ANGLE_MAX.get();

      for (int attempt = 0; attempt < 5; attempt++) {
         double angleDegrees = (random.nextDouble() * 2.0 - 1.0) * maxViewAngle;
         double distance = minDistance + random.nextDouble() * (maxDistance - minDistance);
         double placementYaw = player.getYRot() + angleDegrees;
         double rad = Math.toRadians(placementYaw);
         double targetX = player.getX() - Math.sin(rad) * distance;
         double targetZ = player.getZ() + Math.cos(rad) * distance;
         BlockPos groundPos = findGroundedPos(level, BlockPos.containing(targetX, player.getY(), targetZ));
         if (groundPos != null) {
            spawnLightningSilhouetteAt(level, player, groundPos);
            return true;
         }
      }

      return false;
   }

   private static void spawnLightningSilhouetteAt(ServerLevel level, ServerPlayer player, BlockPos groundPos) {
      PeripheralFigureEntity figure = new PeripheralFigureEntity(
         (EntityType<? extends PeripheralFigureEntity>)PeripheralFigureEntities.PERIPHERAL_FIGURE.get(), level
      );
      double spawnX = groundPos.getX() + 0.5;
      double spawnZ = groundPos.getZ() + 0.5;
      figure.setPos(spawnX, groundPos.getY(), spawnZ);
      double dx = player.getX() - spawnX;
      double dz = player.getZ() - spawnZ;
      double bearingToPlayer = Math.toDegrees(Math.atan2(-dx, dz));
      float yaw = (float)Mth.wrapDegrees(bearingToPlayer);
      figure.setYRot(yaw);
      figure.setYHeadRot(yaw);
      figure.setYBodyRot(yaw);
      figure.setXRot(0.0F);
      figure.setTargetPlayer(player.getUUID());
      figure.anchorHere();
      figure.markAsLightningSilhouette((Integer)PeripheralFigureConfig.LIGHTNING_SILHOUETTE_LIFETIME_TICKS.get());
      level.addFreshEntity(figure);
      ACTIVE_FIGURE_ENTITY_ID.put(player.getUUID(), figure.getId());
   }

   public static boolean hasLineOfSight(Level level, Vec3 from, Vec3 to) {
      BlockHitResult result = level.clip(new ClipContext(from, to, Block.COLLIDER, Fluid.NONE, CollisionContext.empty()));
      return result.getType() == Type.MISS;
   }

   private static BlockPos findGroundedPos(ServerLevel level, BlockPos candidate) {
      MutableBlockPos pos = candidate.mutable();

      for (int i = 0; i <= 8; i++) {
         if (isStandable(level, pos)) {
            return pos.immutable();
         }

         pos.move(0, 1, 0);
      }

      pos.set(candidate);

      for (int i = 0; i < 8; i++) {
         pos.move(0, -1, 0);
         if (isStandable(level, pos)) {
            return pos.immutable();
         }
      }

      return null;
   }

   private static boolean isStandable(ServerLevel level, BlockPos pos) {
      BlockPos belowPos = pos.below();
      BlockState belowState = level.getBlockState(belowPos);
      boolean groundSolid = belowState.isFaceSturdy(level, belowPos, Direction.UP) && !belowState.is(BlockTags.LEAVES) && !belowState.is(BlockTags.LOGS);
      BlockState feetState = level.getBlockState(pos);
      BlockState headState = level.getBlockState(pos.above());
      boolean feetClear = feetState.getCollisionShape(level, pos).isEmpty() && feetState.getFluidState().isEmpty();
      boolean headClear = headState.getCollisionShape(level, pos.above()).isEmpty() && headState.getFluidState().isEmpty();
      return groundSolid && feetClear && headClear;
   }

   private record Candidate(BlockPos groundPos, boolean rightSide, int lightLevel) {
   }
}
