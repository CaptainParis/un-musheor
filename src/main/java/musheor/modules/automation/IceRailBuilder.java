package musheor.modules.automation;

import java.util.ArrayList;
import java.util.List;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import musheor.Musheor;
import musheor.utils.BlockRenderUtils;
import musheor.utils.HighwayUtils;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

public class IceRailBuilder extends Module {
   private final Setting<IceRailBuilder.Axis> axis2 = this.settings
      .getDefaultGroup()
      .add(((Builder)((Builder)new Builder().name("axis")).defaultValue(IceRailBuilder.Axis.Z_NEG)).build());
   BlockPos xAxisOrigin;
   BlockPos zAxisOrigin;
   List<BlockPos> icePositions = new ArrayList<>();
   List<BlockPos> supportPositions = new ArrayList<>();

   public IceRailBuilder() {
      super(Musheor.AUTOMATION, "ice-rail-builder", "Semi-automated ice highway building");
   }

   public void onActivate() {
      if (this.mc.player != null && this.mc.world != null) {
         this.initAxisOrigins((IceRailBuilder.Axis)this.axis2.get());
      }
   }

   @EventHandler
   public void onTickPre(Pre event) {
      if (this.mc.player != null && this.mc.world != null) {
         if (this.xAxisOrigin != null) {
         }

         if (this.zAxisOrigin != null) {
            this.icePositions.clear();
            this.supportPositions.clear();

            for (BlockPos pos : this.collectIcePositions(true)) {
               if (this.mc.world.getBlockState(pos).getBlock() != Blocks.BLUE_ICE) {
                  this.icePositions.add(pos.toImmutable());
               }
            }

            for (BlockPos posx : this.collectSupportPositions(true)) {
               if (this.mc.world.getBlockState(posx).isReplaceable()) {
                  this.supportPositions.add(posx.toImmutable());
               }
            }

            if (!this.supportPositions.isEmpty()) {
               HighwayUtils.placeBlocksOffhand(this.supportPositions, Blocks.NETHERRACK);
               return;
            }

            if (!this.icePositions.isEmpty()) {
               HighwayUtils.placeBlocksOffhand(this.icePositions, Blocks.BLUE_ICE);
               return;
            }
         }
      }
   }

   @EventHandler
   private void onRender3D(Render3DEvent event) {
      if (this.mc.player != null && this.mc.world != null) {
         BlockRenderUtils.renderPositions(event, this.supportPositions, Blocks.NETHERRACK);
         BlockRenderUtils.renderPositions(event, this.icePositions, Blocks.BLUE_ICE);
      }
   }

   private void initAxisOrigins(IceRailBuilder.Axis axis) {
      this.xAxisOrigin = this.zAxisOrigin = null;
      switch (axis) {
         case X_POS:
         case X_NEG:
            this.xAxisOrigin = new BlockPos(0, 115, -200);
            break;
         case Z_POS:
         case Z_NEG:
            this.zAxisOrigin = new BlockPos(-200, 115, 0);
      }
   }

   private List<BlockPos> collectSupportPositions(boolean x) {
      List<BlockPos> positions = new ArrayList<>();
      if (x) {
         for (int i = -2; i < 2; i++) {
            positions.add(new BlockPos(this.mc.player.getBlockX() + i, 116, -201));
            positions.add(new BlockPos(this.mc.player.getBlockX() + i, 116, -198));
            positions.add(new BlockPos(this.mc.player.getBlockX() + i, 113, -199));
         }
      }

      return positions;
   }

   private List<BlockPos> collectIcePositions(boolean x) {
      List<BlockPos> positions = new ArrayList<>();
      if (x) {
         for (int i = -5; i < 5; i++) {
            if ((this.mc.player.getBlockX() + i) % 2 == 0) {
               positions.add(new BlockPos(this.mc.player.getBlockX() + i, 115, -200));
            }
         }
      }

      return positions;
   }

   private static enum Axis {
      X_POS,
      X_NEG,
      Z_POS,
      Z_NEG;}
}
