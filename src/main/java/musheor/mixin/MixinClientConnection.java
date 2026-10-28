package musheor.mixin;

import musheor.modules.hud.HudInfoPlus;
import musheor.modules.tech.ContainerTweaks;
import musheor.utils.system.MusheorSystem;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ButtonClickC2SPacket;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import musheor.utils.PacketBudget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ClientConnection.class})
public abstract class MixinClientConnection {
   @Inject(
      method = {"method_10743"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onSendPacket(Packet<?> packet, CallbackInfo ci) {
      if (packet instanceof ClickSlotC2SPacket || packet instanceof CreativeInventoryActionC2SPacket || packet instanceof ButtonClickC2SPacket) {
         if (ContainerTweaks.INSTANCE.isActive() && (Boolean)MusheorSystem.Manager.noPacketKick.get() && !PacketBudget.canSendInvPacket()) {
            ci.cancel();
            return;
         }

         HudInfoPlus.recordInventoryPacket();
      }

      if (packet instanceof PlayerInteractBlockC2SPacket) {
         HudInfoPlus.recordInteractionPacket();
      }

      HudInfoPlus.recordGlobalPacket();
   }
}
