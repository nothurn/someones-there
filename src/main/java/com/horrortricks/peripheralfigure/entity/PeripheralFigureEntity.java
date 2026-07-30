package com.horrortricks.peripheralfigure.entity;

import com.horrortricks.peripheralfigure.PeripheralFigureConfig;
import com.horrortricks.peripheralfigure.PeripheralFigureEvents;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class PeripheralFigureEntity extends LivingEntity {
   private static final float MAX_HEAD_YAW_OFFSET_DEGREES = 50.0F;
   private static final float MAX_HEAD_PITCH_DEGREES = 50.0F;
   private static final float HEAD_TURN_SPEED_DEGREES_PER_TICK = 10.0F;
   private static final float BODY_TURN_SPEED_DEGREES_PER_TICK = 4.0F;
   private long ageInTicks;
   @Nullable
   private UUID targetPlayerUuid;
   private double anchorX;
   private double anchorY;
   private double anchorZ;
   private boolean anchorSet;
   private boolean lightningSilhouette;
   private int lightningLifetimeTicks;
   private boolean lingerVariant;
   private int lingerTicksElapsed;

   public PeripheralFigureEntity(EntityType<? extends PeripheralFigureEntity> type, Level level) {
      super(type, level);
      this.setNoGravity(true);
      this.noPhysics = true;
      this.blocksBuilding = false;
      this.setSilent(true);
   }

   public void anchorHere() {
      this.anchorX = this.getX();
      this.anchorY = this.getY();
      this.anchorZ = this.getZ();
      this.anchorSet = true;
   }

   public void setTargetPlayer(UUID uuid) {
      this.targetPlayerUuid = uuid;
   }

   public void markAsLightningSilhouette(int lifetimeTicks) {
      this.lightningSilhouette = true;
      this.lightningLifetimeTicks = lifetimeTicks;
   }

   public boolean isLightningSilhouette() {
      return this.lightningSilhouette;
   }

   public void markAsLingerVariant() {
      this.lingerVariant = true;
   }

   public void tick() {
      super.tick();
      if (!this.getDeltaMovement().equals(Vec3.ZERO)) {
         this.setDeltaMovement(Vec3.ZERO);
      }

      if (this.anchorSet && (this.getX() != this.anchorX || this.getY() != this.anchorY || this.getZ() != this.anchorZ)) {
         super.setPos(this.anchorX, this.anchorY, this.anchorZ);
      }

      if (!this.level().isClientSide()) {
         this.ageInTicks++;
         if (this.lightningSilhouette) {
            if (this.ageInTicks >= this.lightningLifetimeTicks) {
               this.discard();
            }
         } else {
            Player target = this.targetPlayerUuid != null ? this.level().getPlayerByUUID(this.targetPlayerUuid) : null;
            if (target == null) {
               this.discard();
            } else if (!PeripheralFigureEvents.hasLineOfSight(this.level(), target.getEyePosition(), this.getEyePosition())) {
               this.discard();
            } else {
               this.updateHeadTracking(target);
               Vec3 toFigure = this.position().add(0.0, this.getEyeHeight() * 0.5, 0.0).subtract(target.getEyePosition()).normalize();
               Vec3 viewVector = target.getViewVector(1.0F);
               double cosAngle = Mth.clamp(toFigure.dot(viewVector), -1.0, 1.0);
               double angleDegrees = Math.toDegrees(Math.acos(cosAngle));
               if (angleDegrees <= (Double)PeripheralFigureConfig.DISAPPEAR_ANGLE_DEGREES.get()) {
                  if (this.lingerVariant) {
                     this.lingerTicksElapsed++;
                     if (this.lingerTicksElapsed >= (Integer)PeripheralFigureConfig.LINGER_VARIANT_DELAY_TICKS.get()) {
                        this.discard();
                     }
                  } else {
                     this.discard();
                  }
               } else {
                  if (this.lingerVariant) {
                     this.lingerTicksElapsed = 0;
                  }

                  if (this.ageInTicks >= (Long)PeripheralFigureConfig.LOOK_AWAY_TIMEOUT_TICKS.get()) {
                     if (this.targetPlayerUuid != null) {
                        PeripheralFigureEvents.onTimeoutDespawn(this.level(), this.targetPlayerUuid, this.random);
                     }

                     this.discard();
                  }
               }
            }
         }
      }
   }

   private void updateHeadTracking(Player target) {
      double dx = target.getX() - this.getX();
      double dz = target.getZ() - this.getZ();
      double dy = target.getEyeY() - this.getEyeY();
      double horizontalDist = Math.sqrt(dx * dx + dz * dz);
      float targetYHeadRot = (float)(Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
      float targetXRot = Mth.clamp((float)(-(Mth.atan2(dy, horizontalDist) * (180.0 / Math.PI))), -50.0F, 50.0F);
      float bodyRot = this.yBodyRot;
      float headOffset = Mth.degreesDifference(bodyRot, targetYHeadRot);
      if (Math.abs(headOffset) > 50.0F) {
         bodyRot = rotateTowards(bodyRot, targetYHeadRot, 4.0F);
         this.setYBodyRot(bodyRot);
         this.setYRot(bodyRot);
      }

      float newHeadRot = rotateTowards(this.getYHeadRot(), targetYHeadRot, 10.0F);
      newHeadRot = Mth.rotateIfNecessary(newHeadRot, bodyRot, 50.0F);
      this.setYHeadRot(newHeadRot);
      this.setXRot(rotateTowards(this.getXRot(), targetXRot, 10.0F));
   }

   private static float rotateTowards(float from, float to, float maxDelta) {
      float diff = Mth.clamp(Mth.degreesDifference(from, to), -maxDelta, maxDelta);
      return from + diff;
   }

   public boolean isInvulnerableTo(DamageSource damageSource) {
      return true;
   }

   public void knockback(double strength, double x, double z) {
   }

   public boolean isPushedByFluid() {
      return false;
   }

   public boolean isPickable() {
      return false;
   }

   public boolean isPushable() {
      return false;
   }

   protected void doPush(Entity entity) {
   }

   public InteractionResult interact(Player player, InteractionHand hand) {
      return InteractionResult.PASS;
   }

   @Nullable
   protected SoundEvent getHurtSound(DamageSource damageSource) {
      return null;
   }

   @Nullable
   protected SoundEvent getDeathSound() {
      return null;
   }

   public Iterable<ItemStack> getArmorSlots() {
      return List.of();
   }

   public ItemStack getItemBySlot(EquipmentSlot slot) {
      return ItemStack.EMPTY;
   }

   public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
   }

   public HumanoidArm getMainArm() {
      return HumanoidArm.RIGHT;
   }

   protected void defineSynchedData(Builder builder) {
      super.defineSynchedData(builder);
   }

   public void addAdditionalSaveData(CompoundTag compound) {
      compound.putLong("AgeInTicks", this.ageInTicks);
      if (this.targetPlayerUuid != null) {
         compound.putUUID("TargetPlayer", this.targetPlayerUuid);
      }
   }

   public void readAdditionalSaveData(CompoundTag compound) {
      this.ageInTicks = compound.getLong("AgeInTicks");
      if (compound.hasUUID("TargetPlayer")) {
         this.targetPlayerUuid = compound.getUUID("TargetPlayer");
      }
   }
}
