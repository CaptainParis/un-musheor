package musheor.mixin;

import musheor.utils.system.MusheorSystem;
import musheor.utils.system.MutualManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Entity.class})
public class MixinPlayerOutline {
   @Inject(
      method = {"method_5851"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void makeGlow(CallbackInfoReturnable<Boolean> cir) {
      if (!(Boolean)MusheorSystem.Manager.disableMutuals.get()) {
         Entity entity = (Entity)(Object)this;
         if (entity instanceof PlayerEntity player && MutualManager.mutualManager.shouldGlow(player) && !MinecraftClient.getInstance().options.hudHidden) {
            cir.setReturnValue(true);
         }
      }
   }

   @Inject(
      method = {"method_22861"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void getTeamColorValue(CallbackInfoReturnable<Integer> cir) {
      if (!(Boolean)MusheorSystem.Manager.disableMutuals.get()) {
         Entity entity = (Entity)(Object)this;
         if (entity instanceof PlayerEntity player) {
            if (MutualManager.mutualManager.isPost(player)) {
               cir.setReturnValue(Formatting.GREEN.getColorValue());
            } else if (MutualManager.mutualManager.isKek(player)) {
               cir.setReturnValue(Formatting.AQUA.getColorValue());
            }
         }
      }
   }
}
