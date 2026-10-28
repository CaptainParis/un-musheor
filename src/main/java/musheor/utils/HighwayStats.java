package musheor.utils;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.modules.Modules;
import musheor.modules.automation.HighwayBuilder;
import musheor.utils.manager.InventoryManager;
import net.minecraft.item.Items;

public class HighwayStats {
   public static int cachedDistanceTravelled = 0;

   public static String formatSectionPercent(int sectionSize) {
      double distanceLeft = remainingInSection(sectionSize);
      double percentage = (sectionSize - distanceLeft) / sectionSize * 100.0;
      return String.format("%.2f", percentage);
   }

   public static int computeDistanceTravelled() {
      HighwayUtils.HighwayDirection direction = HighwayBuilder.getHighwayDirection();
      assert MeteorClient.mc.player != null;

      int distanceTravelled = 0;
      if (direction == HighwayUtils.HighwayDirection.NORTH || direction == HighwayUtils.HighwayDirection.SOUTH) {
         distanceTravelled = Math.abs(MeteorClient.mc.player.getBlockZ()) - Math.abs(HighwayBuilderState.getInstance().getStartZ());
      } else if (direction == HighwayUtils.HighwayDirection.EAST || direction == HighwayUtils.HighwayDirection.WEST) {
         distanceTravelled = Math.abs(MeteorClient.mc.player.getBlockX()) - Math.abs(HighwayBuilderState.getInstance().getStartX());
      }

      if (direction == HighwayUtils.HighwayDirection.NORTHEAST
         || direction == HighwayUtils.HighwayDirection.NORTHWEST
         || direction == HighwayUtils.HighwayDirection.SOUTHEAST
         || direction == HighwayUtils.HighwayDirection.SOUTHWEST) {
         distanceTravelled = Math.abs(Math.abs(MeteorClient.mc.player.getBlockX()) - Math.abs(HighwayBuilderState.getInstance().getStartX()));
      }

      return Math.abs(distanceTravelled);
   }

   public static int getDistanceTravelled() {
      if (((HighwayBuilder)Modules.get().get(HighwayBuilder.class)).isActive()) {
         cachedDistanceTravelled = computeDistanceTravelled();
      }

      return cachedDistanceTravelled;
   }

   public static double getPlacementRate() {
      if (!((HighwayBuilder)Modules.get().get(HighwayBuilder.class)).isActive()) {
         return -1.0;
      } else {
         double obsidianPlaced = HighwayBuilderState.getInstance().getObsidianPlaced();
         double ticksPassed = HighwayBuilderState.getInstance().getTickCount();
         return obsidianPlaced != 0.0 && ticksPassed != 0.0 ? obsidianPlaced / (ticksPassed / 20.0) : 0.0;
      }
   }

   public static double getBreakRate() {
      if (!((HighwayBuilder)Modules.get().get(HighwayBuilder.class)).isActive()) {
         return -1.0;
      } else {
         HighwayBuilderState state = HighwayBuilderState.getInstance();
         double blocksBroken = state.getObsidianBroken() + state.getNetherrackBroken() + state.getEnderChestBroken();
         double ticksPassed = state.getTickCount();
         return blocksBroken != 0.0 && ticksPassed != 0.0 ? blocksBroken / (ticksPassed / 20.0) : 0.0;
      }
   }

   public static int getAvailableObsidian() {
      return InventoryManager.countItemIncludingShulkers(Items.ENDER_CHEST) * 8 + InventoryManager.countItemIncludingShulkers(Items.OBSIDIAN);
   }

   public static double blocksPerSecond(double distanceTravelled, double ticksPassed) {
      double secondsPassed = ticksPassed / 20.0;
      return secondsPassed <= 0.0 ? 0.0 : distanceTravelled / secondsPassed;
   }

   public static double estimateSectionEtaSeconds(int sectionSize) {
      HighwayBuilderState state = HighwayBuilderState.getInstance();
      if (getDistanceTravelled() >= 1 && state.getTickCount() >= 1) {
         long remainingDistance = remainingInSection(sectionSize);
         long distanceTravelled = getDistanceTravelled();
         if (distanceTravelled <= 1L) {
            return -2.0;
         } else {
            return state.getTickCount() <= 200 ? -1.0 : remainingDistance / blocksPerSecond(distanceTravelled, state.getTickCount());
         }
      } else {
         return 0.0;
      }
   }

   public static int remainingInSection(int sectionSize) {
      assert MeteorClient.mc.player != null;

      HighwayUtils.HighwayDirection direction = HighwayBuilder.getHighwayDirection();
      int blockCoordinate = switch (direction) {
         case NORTH, SOUTH -> MeteorClient.mc.player.getBlockZ();
         case EAST, WEST, NORTHEAST, NORTHWEST, SOUTHEAST, SOUTHWEST -> MeteorClient.mc.player.getBlockX();
         case null -> 0;
      };
      int sectionStart = Math.floorDiv(blockCoordinate, sectionSize) * sectionSize;
      int sectionEnd = sectionStart + sectionSize;
      if (direction == HighwayUtils.HighwayDirection.SOUTH
         || direction == HighwayUtils.HighwayDirection.WEST
         || direction == HighwayUtils.HighwayDirection.NORTHEAST
         || direction == HighwayUtils.HighwayDirection.SOUTHEAST) {
         sectionStart = sectionEnd;
      }

      return Math.abs(sectionStart - blockCoordinate);
   }

   public static String formatCompact(int value) {
      if (value >= 1000000) {
         return String.format("%.2fm", value / 1000000.0);
      } else {
         return value >= 1000 ? String.format("%.2fk", value / 1000.0) : String.valueOf(value);
      }
   }

   public static String formatRatePerSecond(double placementsPerSecond) {
      return placementsPerSecond == -1.0 ? "Waiting..." : String.format("%.2f blocks / s", placementsPerSecond);
   }

   public static String formatRatePerHour(double placementsPerSecond) {
      return placementsPerSecond == -1.0 ? "Waiting..." : String.format("%.2f blocks / h", placementsPerSecond * 3600.0);
   }

   public static String formatTravelRatePerSecond(int distanceTravelled) {
      return distanceTravelled < 1
         ? "Waiting..."
         : String.format("%.2f blocks / s", (float)blocksPerSecond(distanceTravelled, HighwayBuilderState.getInstance().getTickCount()));
   }

   public static String formatTravelRatePerHour(int distanceTravelled) {
      return distanceTravelled < 1
         ? "Waiting..."
         : String.format("%.2f blocks / h", (float)blocksPerSecond(distanceTravelled, HighwayBuilderState.getInstance().getTickCount()) * 3600.0F);
   }

   public static String formatBreakRatePerSecond(double breakingPerSecond) {
      return breakingPerSecond == -1.0 ? "Waiting..." : String.format("%.2f blocks / s", breakingPerSecond);
   }

   public static String formatMaterialEta() {
      if (getPlacementRate() == -1.0) {
         return "Waiting...";
      } else {
         long timeLeft = (long)(getAvailableObsidian() / getPlacementRate());
         long hours = timeLeft / 3600L;
         long minutes = timeLeft % 3600L / 60L;
         long seconds = timeLeft % 60L;
         return String.format("%d:%02d:%02d", hours, minutes, seconds);
      }
   }

   public static String formatSectionEta(int sectionSize) {
      if (getPlacementRate() == -1.0) {
         return "Waiting...";
      } else {
         long timeLeft = (long)(remainingInSection(sectionSize) / blocksPerSecond(getDistanceTravelled(), HighwayBuilderState.getInstance().getTickCount()));
         long hours = timeLeft / 3600L;
         long minutes = timeLeft % 3600L / 60L;
         long seconds = timeLeft % 60L;
         return String.format("%d:%02d:%02d", hours, minutes, seconds);
      }
   }

   public static String formatTicksAsClock(long ticks) {
      if (ticks == -1L) {
         return "Calculating...";
      } else if (ticks == -2L) {
         return "Waiting...";
      } else {
         long totalSeconds = ticks / 20L;
         long hours = totalSeconds / 3600L;
         long minutes = totalSeconds % 3600L / 60L;
         long seconds = totalSeconds % 60L;
         return String.format("%d:%02d:%02d", hours, minutes, seconds);
      }
   }
}
