package musheor.mixin;

import musheor.utils.HighwayLogic;
import musheor.utils.manager.RotationManager;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRotationS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ClientPlayNetworkHandler.class})
public class MixinClientPlayNetworkHandler {
   @Unique
   private boolean musheor$keepLook;
   @Unique
   private float musheor$yaw;
   @Unique
   private float musheor$pitch;

   @Unique
   private void musheor$capture() {
      this.musheor$keepLook = HighwayLogic.mc.player != null && RotationManager.isActive();
      if (this.musheor$keepLook) {
         this.musheor$yaw = HighwayLogic.mc.player.getYaw();
         this.musheor$pitch = HighwayLogic.mc.player.getPitch();
      }
   }

   @Unique
   private void musheor$restore() {
      if (this.musheor$keepLook) {
         this.musheor$keepLook = false;
         if (HighwayLogic.mc.player != null) {
            HighwayLogic.mc.player.setYaw(this.musheor$yaw);
            HighwayLogic.mc.player.setPitch(this.musheor$pitch);
         }
      }
   }

   @Inject(
      method = {"method_11157"},
      at = {@At("HEAD")}
   )
   private void musheor$captureOnTeleport(PlayerPositionLookS2CPacket packet, CallbackInfo ci) {
      this.musheor$capture();
   }

   @Inject(
      method = {"method_11157"},
      at = {@At("RETURN")}
   )
   private void musheor$restoreOnTeleport(PlayerPositionLookS2CPacket packet, CallbackInfo ci) {
      this.musheor$restore();
   }

   @Inject(
      method = {"method_64554"},
      at = {@At("HEAD")}
   )
   private void musheor$captureOnRotation(PlayerRotationS2CPacket packet, CallbackInfo ci) {
      this.musheor$capture();
   }

   @Inject(
      method = {"method_64554"},
      at = {@At("RETURN")}
   )
   private void musheor$restoreOnRotation(PlayerRotationS2CPacket packet, CallbackInfo ci) {
      this.musheor$restore();
   }
}
