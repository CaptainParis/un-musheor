package musheor.modules.features;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BlockListSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import musheor.Musheor;
import musheor.utils.manager.InventoryManager;
import musheor.utils.system.MusheorSystem;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import musheor.utils.HighwayBuilderState;
import musheor.utils.BlockPlacer;
import musheor.utils.PacketBudget;

public class GrimScaffold extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private final Setting<List<Block>> allowedBlocks = this.sgGeneral
      .add(
         ((Builder)((Builder)new Builder().name("allowed-blocks")).description("Which blocks scaffold is allowed to place"))
            .defaultValue(new Block[]{Blocks.OBSIDIAN, Blocks.NETHERRACK})
            .build()
      );
   private final Setting<Boolean> expand2 = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("expand"))
                  .description("Extend the scaffold area beyond directly under your feet"))
               .defaultValue(false))
            .build()
      );
   private final Setting<Double> expandRange = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("expand-range"))
                  .description("How many blocks to expand the scaffold area"))
               .defaultValue(2.0)
               .sliderRange(1.0, 5.0)
               .decimalPlaces(1)
               .visible(this.expand2::get))
            .build()
      );
   private final Setting<Boolean> expandForwardOnly = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("expand-forward-only"))
                     .description("Only expand in the direction you are currently facing"))
                  .defaultValue(true))
               .visible(this.expand2::get))
            .build()
      );
   private final Setting<Boolean> render2 = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("render"))
                  .description("Renders an overlay where blocks will be placed"))
               .defaultValue(true))
            .build()
      );
   private final List<BlockPos> renderPositions = new ArrayList<>();
   private Block placeBlock = null;

   public GrimScaffold() {
      super(Musheor.MAIN, "simple-scaffold", "Place blocks under your feet, even if you are mid-air");
   }

   public void onActivate() {
      this.renderPositions.clear();
      this.placeBlock = null;
   }

   public void onDeactivate() {
      this.renderPositions.clear();
      this.placeBlock = null;
   }

   @EventHandler
   private void onTickPost(Post event) {
      if (mc.player != null && mc.world != null) {
         this.placeBlock = this.findPlaceBlock();
         this.renderPositions.clear();
         List<BlockPos> toPlace = this.collectPlacePositions();
         toPlace.removeIf(pos -> !BlockUtils.canPlace(pos, true));
         if ((Boolean)this.render2.get()) {
            this.renderPositions.addAll(toPlace);
         }

         if (this.placeBlock != null && !toPlace.isEmpty()) {
            if (!(mc.player.getMainHandStack().getItem() instanceof BlockItem bi && bi.getBlock() == this.placeBlock)) {
               InventoryManager.swapToItemSilent(this.placeBlock.asItem(), (Boolean)MusheorSystem.Manager.silentPlace.get());
            }

            for (BlockPos pos : toPlace) {
               if (!PacketBudget.canPlace()) {
                  break;
               }

               BlockPlacer.place(pos, Direction.UP, HighwayBuilderState.getInstance().getPlacementType(), (Boolean)MusheorSystem.Manager.silentPlace.get());
            }
         }
      }
   }

   private Block findPlaceBlock() {
      ItemStack main = mc.player.getMainHandStack();
      if (main.getItem() instanceof BlockItem bi && ((List)this.allowedBlocks.get()).contains(bi.getBlock()) && !main.isEmpty()) {
         return bi.getBlock();
      } else {
         for (int i = 0; i < 9; i++) {
            ItemStack s = mc.player.getInventory().getStack(i);
            if (s.getItem() instanceof BlockItem bi && ((List)this.allowedBlocks.get()).contains(bi.getBlock()) && !s.isEmpty()) {
               return bi.getBlock();
            }
         }

         return null;
      }
   }

   private List<BlockPos> collectPlacePositions() {
      List<BlockPos> positions = new ArrayList<>();
      Set<BlockPos> seen = new HashSet<>();
      BlockPos below = mc.player.getBlockPos().down();
      if (seen.add(below)) {
         positions.add(below);
      }

      if ((Boolean)this.expand2.get()) {
         int range = (int)Math.ceil((Double)this.expandRange.get());
         double rangeSq = (Double)this.expandRange.get() * (Double)this.expandRange.get();
         Direction facing = mc.player.getHorizontalFacing();
         if ((Boolean)this.expandForwardOnly.get()) {
            for (int i = 1; i <= range; i++) {
               BlockPos p = new BlockPos(below.getX() + facing.getOffsetX() * i, below.getY(), below.getZ() + facing.getOffsetZ() * i);
               if (seen.add(p)) {
                  positions.add(p);
               }
            }
         } else {
            for (int x = -range; x <= range; x++) {
               for (int z = -range; z <= range; z++) {
                  if ((x != 0 || z != 0) && !(x * x + z * z > rangeSq)) {
                     BlockPos p = below.add(x, 0, z);
                     if (seen.add(p)) {
                        positions.add(p);
                     }
                  }
               }
            }
         }
      }

      return positions;
   }

   @EventHandler
   private void onRender3D(Render3DEvent event) {
      if ((Boolean)this.render2.get() && !this.renderPositions.isEmpty()) {
         Block b = this.placeBlock != null ? this.placeBlock : Blocks.OBSIDIAN;
         musheor.utils.BlockRenderUtils.renderPositions(event, this.renderPositions, b);
      }
   }
}
