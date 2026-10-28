package musheor.modules.hud;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import musheor.Musheor;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class DubCounter extends HudElement {
   private final MinecraftClient mc = MinecraftClient.getInstance();
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgRender = this.settings.createGroup("Render");
   private final Setting<Boolean> doubleChests = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("double-chests")).description("Counts connected double chests (including trapped ones)."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> singleChests = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("single-chests")).description("Counts single, unconnected chests (including trapped ones)."))
               .defaultValue(false))
            .build()
      );
   private final Setting<List<BlockEntityType<?>>> otherStorages = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.StorageBlockListSetting.Builder)((meteordevelopment.meteorclient.settings.StorageBlockListSetting.Builder)new meteordevelopment.meteorclient.settings.StorageBlockListSetting.Builder()
                  .name("other-storages"))
               .description("Other storage blocks to count. Chests are handled by the settings above."))
            .defaultValue(new BlockEntityType[0])
            .build()
      );
   private final Setting<Boolean> separateLines = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("separate-lines")).description("Shows a line per storage type instead of a single total."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Integer> updateDelay = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("update-delay"))
                  .description("Ticks between rescans of the loaded chunks."))
               .defaultValue(10))
            .min(1)
            .sliderRange(1, 100)
            .build()
      );
   private final Setting<Double> textScale = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                  .name("text-scale"))
               .description("Scale of the text."))
            .defaultValue(1.0)
            .sliderRange(0.1, 3.0)
            .decimalPlaces(1)
            .build()
      );
   private final Setting<SettingColor> labelColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("label-color"))
               .description("Color of the labels."))
            .defaultValue(new SettingColor(255, 255, 255, 255))
            .build()
      );
   private final Setting<SettingColor> valueColor = this.sgRender
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder()
                  .name("value-color"))
               .description("Color of the counts."))
            .defaultValue(new SettingColor(175, 175, 175, 255))
            .build()
      );
   public static final HudElementInfo<DubCounter> INFO = new HudElementInfo(
      Musheor.MUSHEOR_HUD, "dub-counter", "Counts connected double chests (and optionally other storage) in render distance", DubCounter::new
   );
   private int dubCount;
   private int singleChestCount;
   private final Map<BlockEntityType<?>, Integer> otherCounts = new LinkedHashMap<>();
   private int ticksUntilScan;

   public DubCounter() {
      super(INFO);
   }

   public void tick(HudRenderer renderer) {
      if (this.mc.world != null) {
         if (--this.ticksUntilScan <= 0) {
            this.ticksUntilScan = (Integer)this.updateDelay.get();
            this.rescan();
         }
      }
   }

   private void rescan() {
      this.dubCount = 0;
      this.singleChestCount = 0;
      this.otherCounts.clear();

      for (BlockEntityType<?> type : (List<BlockEntityType<?>>)(List)this.otherStorages.get()) {
         if (!isChestType(type)) {
            this.otherCounts.put(type, 0);
         }
      }

      for (BlockEntity be : Utils.blockEntities()) {
         if (be instanceof ChestBlockEntity) {
            BlockState state = be.getCachedState();
            if (state.contains(ChestBlock.CHEST_TYPE)) {
               ChestType typex = (ChestType)state.get(ChestBlock.CHEST_TYPE);
               if (typex == ChestType.SINGLE) {
                  this.singleChestCount++;
               } else if (typex == ChestType.LEFT || !this.isPartnerChunkLoaded(be.getPos(), state)) {
                  this.dubCount++;
               }
            }
         } else {
            this.otherCounts.computeIfPresent(be.getType(), (t, n) -> n + 1);
         }
      }
   }

   private boolean isPartnerChunkLoaded(BlockPos pos, BlockState state) {
      BlockPos partner = pos.offset(ChestBlock.getFacing(state));
      return this.mc.world.getChunkManager().isChunkLoaded(partner.getX() >> 4, partner.getZ() >> 4);
   }

   private static boolean isChestType(BlockEntityType<?> type) {
      return type == BlockEntityType.CHEST || type == BlockEntityType.TRAPPED_CHEST;
   }

   public void render(HudRenderer renderer) {
      List<String[]> rows = new ArrayList<>();
      int total = 0;
      if ((Boolean)this.doubleChests.get()) {
         rows.add(new String[]{"Dubs: ", Integer.toString(this.dubCount)});
         total += this.dubCount;
      }

      if ((Boolean)this.singleChests.get()) {
         rows.add(new String[]{"Single Chests: ", Integer.toString(this.singleChestCount)});
         total += this.singleChestCount;
      }

      for (Entry<BlockEntityType<?>, Integer> entry : this.otherCounts.entrySet()) {
         rows.add(new String[]{formatStorageName(entry.getKey()) + ": ", Integer.toString(entry.getValue())});
         total += entry.getValue();
      }

      if (!(Boolean)this.separateLines.get() || rows.isEmpty()) {
         String label = rows.size() == 1 ? rows.get(0)[0] : "Storage: ";
         rows.clear();
         rows.add(new String[]{label, Integer.toString(total)});
      }

      double lineHeight = renderer.textHeight(true, (Double)this.textScale.get());
      double width = 0.0;
      double cy = this.y;

      for (String[] row : rows) {
         double labelW = renderer.textWidth(row[0], true, (Double)this.textScale.get());
         renderer.text(row[0], this.x, cy, (Color)this.labelColor.get(), true, (Double)this.textScale.get());
         renderer.text(row[1], this.x + labelW, cy, (Color)this.valueColor.get(), true, (Double)this.textScale.get());
         width = Math.max(width, labelW + renderer.textWidth(row[1], true, (Double)this.textScale.get()));
         cy += lineHeight;
      }

      this.setSize(width, cy - this.y);
   }

   private static String formatStorageName(BlockEntityType<?> type) {
      Identifier id = Registries.BLOCK_ENTITY_TYPE.getId(type);
      if (id == null) {
         return "Unknown";
      } else {
         StringBuilder sb = new StringBuilder();

         for (String word : id.getPath().split("_")) {
            if (!word.isEmpty()) {
               if (!sb.isEmpty()) {
                  sb.append(' ');
               }

               sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
            }
         }

         return sb.toString();
      }
   }
}
