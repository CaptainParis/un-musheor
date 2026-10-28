package musheor.utils;

import meteordevelopment.discordipc.DiscordIPC;
import meteordevelopment.discordipc.RichPresence;
import musheor.utils.system.MusheorSystem;

public class DiscordRPC {
   private static final long APPLICATION_ID = 1346947991059300434L;
   private static final RichPresence presence = new RichPresence();

   public static void start() {
      DiscordIPC.start(1346947991059300434L, null);
      presence.setStart(System.currentTimeMillis() / 1000L);
      presence.setLargeImage("musheor_logo", "Musheor Highway Builder");
      update();
   }

   public static void stop() {
      DiscordIPC.stop();
   }

   public static void update() {
      if (musheor.utils.HighwayBuilderState.getInstance().getTickCount() > 20) {
         presence.setDetails(formatDetails());
         presence.setState(formatState());
         DiscordIPC.setActivity(presence);
         MusheorSystem.debug("Updated RPC.");
      }
   }

   private static String formatDetails() {
      return String.format(
         "Obsidian placed | mined: %s | %s",
         HighwayStats.formatCompact(musheor.utils.HighwayBuilderState.getInstance().getObsidianPlaced()),
         HighwayStats.formatCompact(musheor.utils.HighwayBuilderState.getInstance().getObsidianBroken())
      );
   }

   private static String formatState() {
      return String.format(
         "Hour: %s placed | %s mined",
         HighwayStats.formatCompact(musheor.utils.HighwayBuilderState.getInstance().getPlacedPerHour()),
         HighwayStats.formatCompact(musheor.utils.HighwayBuilderState.getInstance().getMinedPerHour())
      );
   }
}
