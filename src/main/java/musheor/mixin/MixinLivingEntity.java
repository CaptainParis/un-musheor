package musheor.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import musheor.modules.automation.KekBounce;
import musheor.modules.features.HorseFly;
import musheor.modules.features.NoMoreJumpDelay;
import musheor.utils.HighwayLogic;
import musheor.utils.manager.RotationManager;
import musheor.utils.manager.ElytraFireworkManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LivingEntity.class})
public abstract class MixinLivingEntity extends Entity {
   @Shadow
   private int field_6228;
   @Unique
   private boolean musheor$travelSpoof;
   @Unique
   private float musheor$travelYaw;
   @Unique
   private float musheor$travelPitch;
   @Unique
   private boolean musheor$jumpSpoof;
   @Unique
   private float musheor$jumpYaw;
   @Unique
   private float musheor$jumpPitch;

   public MixinLivingEntity(EntityType<?> type, World world) {
      super(type, world);
   }

   @Shadow
   public abstract Brain<?> method_18868();

   @Unique
   private boolean musheor$isLocalPlayer() {
      return HighwayLogic.mc.player != null && (Object)this == HighwayLogic.mc.player;
   }

   @Inject(
      method = {"method_6091"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void musheor$horseFlyTravel(Vec3d movementInput, CallbackInfo ci) {
      HorseFly horseFly = Modules.get() == null ? null : (HorseFly)Modules.get().get(HorseFly.class);
      if (horseFly != null && horseFly.tryFlyTravel((LivingEntity)(Object)this)) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"method_6091"},
      at = {@At("HEAD")}
   )
   private void musheor$travelHead(Vec3d movementInput, CallbackInfo ci) {
      this.musheor$travelSpoof = false;
      if (this.musheor$isLocalPlayer() && RotationManager.isActive()) {
         this.musheor$travelYaw = this.getYaw();
         this.musheor$travelPitch = this.getPitch();
         this.setYaw(RotationManager.yawOr(this.musheor$travelYaw));
         this.setPitch(RotationManager.pitchOr(this.musheor$travelPitch));
         this.musheor$travelSpoof = true;
      }
   }

   @Inject(
      method = {"method_6091"},
      at = {@At("RETURN")}
   )
   private void musheor$travelTail(Vec3d movementInput, CallbackInfo ci) {
      if (this.musheor$travelSpoof) {
         this.setYaw(this.musheor$travelYaw);
         this.setPitch(this.musheor$travelPitch);
         this.musheor$travelSpoof = false;
      }
   }

   @Inject(
      method = {"method_6043"},
      at = {@At("HEAD")}
   )
   private void musheor$jumpHead(CallbackInfo ci) {
      this.musheor$jumpSpoof = false;
      if (this.musheor$isLocalPlayer() && RotationManager.isActive()) {
         this.musheor$jumpYaw = this.getYaw();
         this.musheor$jumpPitch = this.getPitch();
         this.setYaw(RotationManager.yawOr(this.musheor$jumpYaw));
         this.setPitch(RotationManager.pitchOr(this.musheor$jumpPitch));
         this.musheor$jumpSpoof = true;
      }
   }

   @Inject(
      method = {"method_6043"},
      at = {@At("RETURN")}
   )
   private void musheor$jumpTail(CallbackInfo ci) {
      if (this.musheor$jumpSpoof) {
         this.setYaw(this.musheor$jumpYaw);
         this.setPitch(this.musheor$jumpPitch);
         this.musheor$jumpSpoof = false;
      }
   }

   @Inject(
      at = {@At("HEAD")},
      method = {"method_6007"}
   )
   private void tickMovement(CallbackInfo ci) {
      NoMoreJumpDelay njd = (NoMoreJumpDelay)Modules.get().get(NoMoreJumpDelay.class);
      KekBounce bounce = (KekBounce)Modules.get().get(KekBounce.class);
      if (HighwayLogic.mc.player != null
         && HighwayLogic.mc.player.getBrain().equals(this.method_18868())
         && (njd != null && njd.isActive() || ElytraFireworkManager.isBoostActive() || bounce != null && bounce.isBouncing())) {
         this.field_6228 = 0;
      }
   }
}
