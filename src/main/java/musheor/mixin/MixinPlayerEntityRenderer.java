package musheor.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import musheor.modules.features.MoreTags;
import musheor.utils.HighwayLogic;
import musheor.utils.manager.RotationManager;
import musheor.utils.manager.ElytraFireworkManager;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.PlayerLikeEntity;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({PlayerEntityRenderer.class})
public class MixinPlayerEntityRenderer {
   @Inject(
      method = {"method_62604"},
      at = {@At("TAIL")}
   )
   private void applyServerRotation(PlayerLikeEntity player, PlayerEntityRenderState state, float tickDelta, CallbackInfo ci) {
      if (player == HighwayLogic.mc.player) {
         if (RotationManager.isActive()) {
            Float yaw = RotationManager.getPendingYaw();
            Float pitch = RotationManager.getPendingPitch();
            if (yaw != null) {
               state.bodyYaw = yaw;
            }

            if (pitch != null) {
               state.pitch = pitch;
            }

            state.relativeHeadYaw = 0.0F;
            state.flyingRotation = 0.0F;
         }

         if (ElytraFireworkManager.isLaunched() && !player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA)) {
            state.pose = EntityPose.STANDING;
            state.isGliding = false;
            state.isSwimming = false;
            state.leaningPitch = 0.0F;
            state.flyingRotation = 0.0F;
            state.glidingTicks = 0.0F;
            state.applyFlyingRotation = false;
         }
      }
   }

   @Inject(
      method = {"method_4213"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void cancelDefaultNametag(
      PlayerEntityRenderState state, MatrixStack matrixStack, OrderedRenderCommandQueue queue, CameraRenderState cameraRenderState, CallbackInfo ci
   ) {
      MoreTags module = (MoreTags)Modules.get().get(MoreTags.class);
      if (module != null && module.isActive() && (Boolean)module.enabled2.get()) {
         ci.cancel();
      }
   }
}
