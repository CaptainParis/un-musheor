package musheor.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import musheor.modules.features.KekMine;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ClientPlayerInteractionManager.class})
public class MixinClientPlayerInteractionManager {
   @Inject(
      method = {"method_2910"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onAttackBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
      KekMine kekMine = (KekMine)Modules.get().get(KekMine.class);
      if (kekMine.isActive() && !MinecraftClient.getInstance().player.isCreative()) {
         if (!BlockUtils.canInstaBreak(pos) && (!kekMine.shouldSilentSwap() || !KekMine.canInstaBreak(pos))) {
            kekMine.queueOrStartBreak(pos);
         } else {
            KekMine.instantBreakAt(pos);
         }

         cir.setReturnValue(true);
      }
   }

   @Inject(
      method = {"method_2902"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onUpdateBlockBreakingProgress(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
      KekMine kekMine = (KekMine)Modules.get().get(KekMine.class);
      if (kekMine.isActive() && !MinecraftClient.getInstance().player.isCreative()) {
         if (!BlockUtils.canInstaBreak(pos) && (!kekMine.shouldSilentSwap() || !KekMine.canInstaBreak(pos))) {
            kekMine.queueOrStartBreak(pos);
         } else {
            KekMine.instantBreakAt(pos);
         }

         cir.setReturnValue(true);
      }
   }
}
