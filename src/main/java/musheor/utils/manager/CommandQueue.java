package musheor.utils.manager;

import java.util.ArrayDeque;
import musheor.utils.HighwayLogic;
import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket;

public class CommandQueue {
   private static final ArrayDeque<Integer> pendingPongs = new ArrayDeque<>();
   private static volatile boolean capturing;

   public static synchronized void beginCapture() {
      capturing = true;
   }

   public static synchronized boolean isCapturing() {
      return capturing;
   }

   public static synchronized boolean enqueue(int parameter) {
      if (!capturing) {
         return false;
      } else {
         pendingPongs.addLast(parameter);
         return true;
      }
   }

   public static synchronized void flush() {
      capturing = false;
      if (!pendingPongs.isEmpty()) {
         if (HighwayLogic.mc.getNetworkHandler() == null) {
            pendingPongs.clear();
         } else {
            while (!pendingPongs.isEmpty()) {
               HighwayLogic.mc.getNetworkHandler().sendPacket(new CommonPongC2SPacket(pendingPongs.pollFirst()));
            }
         }
      }
   }

   public static synchronized void clear() {
      capturing = false;
      pendingPongs.clear();
   }
}
