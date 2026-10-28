package musheor.utils;

import java.util.HashSet;
import java.util.Set;
import meteordevelopment.meteorclient.systems.modules.Modules;

public final class EchestSlotLocks {
   private static final Set<Integer> lockedSlots = new HashSet<>();

   public static boolean isUnlocked(int slot) {
      return slot >= 0 && slot < 27 && !lockedSlots.contains(slot);
   }

   public static boolean isLocked(int slot) {
      return lockedSlots.contains(slot);
   }

   public static void unlock(int slot) {
      if (slot >= 0 && slot < 27) {
         lockedSlots.remove(slot);
      }
   }

   public static void lock(int slot) {
      if (slot >= 0 && slot < 27) {
         lockedSlots.add(slot);
      }
   }

   public static int toMask() {
      int mask = 0;

      for (int slot : lockedSlots) {
         if (slot >= 0 && slot < 27) {
            mask |= 1 << slot;
         }
      }

      return mask;
   }

   public static void fromMask(int mask) {
      lockedSlots.clear();

      for (int slot = 0; slot < 27; slot++) {
         if ((mask & 1 << slot) != 0) {
            lockedSlots.add(slot);
         }
      }
   }

   public static void saveModules() {
      Modules.get().save();
   }
}
