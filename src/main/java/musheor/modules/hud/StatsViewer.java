package musheor.modules.hud;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.DoubleSetting.Builder;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import musheor.Musheor;
import musheor.modules.automation.HighwayBuilder;
import musheor.utils.HighwayStats;
import net.minecraft.client.MinecraftClient;
import musheor.utils.HighwayBuilderState;

public class StatsViewer extends HudElement {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   public static final HudElementInfo<StatsViewer> INFO = new HudElementInfo(
      Musheor.MUSHEOR_HUD, "stats-viewer", "View your stats while using the HighwayBuilder module.", StatsViewer::new
   );
   private final Setting<Double> scale2 = this.sgGeneral
      .add(((Builder)((Builder)new Builder().name("scale")).description("I mean cmon do I really have to explain??")).defaultValue(1.0).build());
   private final Setting<Integer> sectionSize = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("section-size"))
                  .description("Distance or size of a single highway section"))
               .defaultValue(10000))
            .build()
      );
   private final Setting<Boolean> displaySession = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("display-session"))
                  .description("Display statistics of the current session."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> displayBlocks = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("display-blocks"))
                  .description("Display statistics of blocks mined and placed."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> displayPerformance = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("display-performance"))
                  .description("Display statistics of paver's performance."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> displayInfo = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("display-info"))
                  .description("Display extra information about the environment, inventory and ETA."))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> displayLifetime = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("display-lifetime"))
                  .description("Display lifetime statistics about the paver."))
               .defaultValue(true))
            .build()
      );
   private final Setting<SettingColor> prefixColor = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("prefix-color"))
               .description("Text color of the hud info lines."))
            .defaultValue(new SettingColor(255, 255, 225, 255))
            .build()
      );
   private final Setting<SettingColor> valueColor = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("value-color"))
               .description("Text color of the hud info lines."))
            .defaultValue(new SettingColor(255, 0, 208, 255))
            .build()
      );

   public StatsViewer() {
      super(INFO);
   }

   private String[][] buildLines() {
      List<String[]> lines = new ArrayList<>();
      HighwayBuilderState state = HighwayBuilderState.getInstance();
      String[][] session = new String[][]{
         {"Latest session: ", ""},
         {"  Runtime: ", HighwayStats.formatTicksAsClock((long)state.getTickCount())},
         {"  Direction: ", String.valueOf(HighwayBuilder.getHighwayDirection()).toLowerCase()},
         {"  Distance travelled: ", String.valueOf(HighwayStats.getDistanceTravelled())}
      };
      String[][] blocks = new String[][]{
         {"Blocks", ""},
         {"  Obsidian Placed: ", HighwayStats.formatCompact(state.getObsidianPlaced())},
         {"  Obsidian Mined: ", HighwayStats.formatCompact(state.getObsidianBroken())},
         {"  Enderchests mined: ", HighwayStats.formatCompact(state.getEnderChestBroken())},
         {"  Netherrack mined: ", HighwayStats.formatCompact(state.getNetherrackBroken())},
         {"  Total mined: ", HighwayStats.formatCompact(state.getObsidianBroken() + state.getNetherrackBroken() + state.getEnderChestBroken())}
      };
      String[][] performance = new String[][]{
         {"Performance", ""},
         {"  Placements / s: ", HighwayStats.formatRatePerSecond(HighwayStats.getPlacementRate())},
         {"  Placements / h: ", HighwayStats.formatRatePerHour(HighwayStats.getPlacementRate())},
         {"  Breaks / s: ", HighwayStats.formatBreakRatePerSecond(HighwayStats.getBreakRate())},
         {"  Distance / s: ", HighwayStats.formatTravelRatePerSecond(HighwayStats.getDistanceTravelled())},
         {"  Distance / h: ", HighwayStats.formatTravelRatePerHour(HighwayStats.getDistanceTravelled())}
      };
      String[][] info = new String[][]{
         {"Info", ""},
         {"  Distance to next section: ", String.valueOf(HighwayStats.remainingInSection((Integer)this.sectionSize.get()))},
         {"  Percentage completed: ", HighwayStats.formatSectionPercent((Integer)this.sectionSize.get())},
         {"  Material Count: ", String.valueOf(HighwayStats.getAvailableObsidian())},
         {"  ETA to next section: ", HighwayStats.formatSectionEta((Integer)this.sectionSize.get())},
         {"  ETA to next refill: ", HighwayStats.formatMaterialEta()}
      };
      String[][] lifetime = new String[][]{
         {"Lifetime: ", ""},
         {"  Obsidian Placed: ", HighwayStats.formatCompact(state.getPlacedPerHour())},
         {"  Obsidian Mined: ", HighwayStats.formatCompact(state.getMinedPerHour())},
         {"  Enderchests mined: ", HighwayStats.formatCompact(state.getEchestsPerHour())},
         {"  Netherrack mined: ", HighwayStats.formatCompact(state.getNetherrackPerHour())},
         {"  Total mined: ", HighwayStats.formatCompact(state.getTotalBrokenPerHour())}
      };
      if ((Boolean)this.displaySession.get()) {
         lines.addAll(Arrays.asList(session));
      }

      if ((Boolean)this.displayBlocks.get()) {
         lines.addAll(Arrays.asList(blocks));
      }

      if ((Boolean)this.displayPerformance.get()) {
         lines.addAll(Arrays.asList(performance));
      }

      if ((Boolean)this.displayInfo.get()) {
         lines.addAll(Arrays.asList(info));
      }

      if ((Boolean)this.displayLifetime.get()) {
         lines.addAll(Arrays.asList(lifetime));
      }

      return lines.toArray(new String[0][]);
   }

   public void render(HudRenderer renderer) {
      if (MinecraftClient.getInstance().player != null && MinecraftClient.getInstance().world != null) {
         String[][] lines = this.buildLines();
         double lineHeight = renderer.textHeight(true, (Double)this.scale2.get());
         double width = 0.0;

         for (String[] line : lines) {
            double lineWidth = renderer.textWidth(line[0], true)
               + renderer.textWidth(": ", true)
               + renderer.textWidth(line[1], true, (Double)this.scale2.get());
            width = Math.max(width, lineWidth);
         }

         this.setSize(width, lineHeight * lines.length);
         double currentY = this.y;

         for (String[] line : lines) {
            double currentX = this.x;
            renderer.text(line[0], currentX, currentY, (Color)this.prefixColor.get(), true, (Double)this.scale2.get());
            currentX += renderer.textWidth(line[0], true, (Double)this.scale2.get());
            renderer.text("", currentX, currentY, (Color)this.prefixColor.get(), true, (Double)this.scale2.get());
            currentX += renderer.textWidth("", true, (Double)this.scale2.get());
            if (line[0].equals("Progress")) {
               String percentageText = line[1];
               String doneText = " done";
               double percentageWidth = renderer.textWidth(percentageText, true);
               renderer.textWidth(doneText, true, (Double)this.scale2.get());
               renderer.text(percentageText, currentX, currentY, (Color)this.valueColor.get(), true, (Double)this.scale2.get());
               currentX += percentageWidth;
               renderer.text(doneText, currentX, currentY, (Color)this.prefixColor.get(), true, (Double)this.scale2.get());
            } else {
               renderer.text(line[1], currentX, currentY, (Color)this.valueColor.get(), true, (Double)this.scale2.get());
            }

            currentY += lineHeight;
         }
      }
   }
}
