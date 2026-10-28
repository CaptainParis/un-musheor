package musheor.utils;

import meteordevelopment.meteorclient.systems.modules.Modules;
import musheor.modules.features.CreativeFly;
import musheor.modules.hud.HudInfoPlus;
import musheor.utils.system.MusheorSystem;
import net.minecraft.client.MinecraftClient;

public class PacketBudget {
   public static boolean canPlace() {
      if (MinecraftClient.getInstance().player.isCreative()) {
         return true;
      } else {
         return ((CreativeFly)Modules.get().get(CreativeFly.class)).isActive()
            ? HudInfoPlus.getInteractionPacketCount() < 8
            : HudInfoPlus.getInteractionPacketCount() < 9;
      }
   }

   public static boolean canSendInvPacket() {
      return MinecraftClient.getInstance().player.isCreative()
         ? true
         : HudInfoPlus.getInventoryPacketCount() < (Integer)MusheorSystem.Manager.invPacketLimit.get();
   }

   public static int getInvPacketCount() {
      return HudInfoPlus.getInventoryPacketCount();
   }

   public static int getRemainingInvBudget() {
      return (Integer)MusheorSystem.Manager.invPacketLimit.get() - getInvPacketCount();
   }
}
