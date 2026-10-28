package musheor.mixin;

import musheor.utils.manager.CommandQueue;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.network.packet.s2c.common.CommonPingS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ClientCommonNetworkHandler.class})
public class MixinClientCommonNetworkHandler {
   @Inject(
      method = {"method_52783"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_8673;method_52787(Lnet/minecraft/class_2596;)V"
      )},
      cancellable = true
   )
   private void musheor$holdPong(CommonPingS2CPacket packet, CallbackInfo ci) {
      if (CommandQueue.enqueue(packet.getParameter())) {
         ci.cancel();
      }
   }
}
