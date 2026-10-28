package musheor.modules.features;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import musheor.Musheor;
import musheor.utils.BlockRenderUtils;
import musheor.utils.HighwayUtils;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BlockItem;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class AirPlace extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private final Setting<Boolean> render2 = this.sgGeneral
      .add(
         ((Builder)((Builder)((Builder)new Builder().name("render")).description("Renders an overlay where the block will be placed.")).defaultValue(true))
            .build()
      );
   private final Setting<Boolean> customRange = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("custom-range")).description("Use custom range for air place.")).defaultValue(false)).build());
   private final Setting<Double> range2 = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("range"))
                  .description("Custom range to place at."))
               .visible(this.customRange::get))
            .defaultValue(4.5)
            .min(0.0)
            .sliderMax(6.0)
            .build()
      );
   private BlockPos placePos = null;
   private boolean wasUsePressed = false;
   private int holdTicks = 0;

   public AirPlace() {
      super(Musheor.MAIN, "airplace", "Place blocks mid-air");
   }

   public void onActivate() {
      this.wasUsePressed = false;
      this.holdTicks = 0;
      this.placePos = null;
   }

   @EventHandler
   private void onTickPost(Post event) {
      if (mc.player != null && mc.world != null) {
         this.placePos = this.findPlacePos();
         boolean pressed = mc.options.useKey.isPressed();
         boolean justPressed = pressed && !this.wasUsePressed;
         this.wasUsePressed = pressed;
         if (!pressed) {
            this.holdTicks = 0;
         } else if (this.placePos != null) {
            if (mc.player.getMainHandStack().getItem() instanceof BlockItem
               || mc.player.getMainHandStack().getItem() instanceof SpawnEggItem) {
               if (BlockUtils.canPlace(this.placePos, true)) {
                  if (justPressed) {
                     this.holdTicks = 0;
                     this.placeAt(this.placePos);
                  } else {
                     this.holdTicks++;
                     if (this.holdTicks >= 4) {
                        this.holdTicks = 0;
                        this.placeAt(this.placePos);
                     }
                  }
               }
            }
         }
      }
   }

   private BlockPos findPlacePos() {
      if (mc.player != null && mc.world != null) {
         double r = this.customRange.get() ? (Double)this.range2.get() : mc.player.getBlockInteractionRange();
         if (mc.getCameraEntity() == null) {
            return null;
         } else if (mc.getCameraEntity().raycast(r, 0.0F, false) instanceof BlockHitResult bhr) {
            BlockPos hitPos = bhr.getBlockPos();
            if (mc.world.getBlockState(hitPos).isReplaceable()) {
               return hitPos;
            } else {
               BlockPos adjacent = hitPos.offset(bhr.getSide());
               return mc.world.getBlockState(adjacent).isReplaceable() ? adjacent : null;
            }
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   private void placeAt(BlockPos pos) {
      BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(pos), Direction.DOWN, pos, false);
      HighwayUtils.swapOffhand();
      HighwayUtils.sendPlacePacket(Hand.OFF_HAND, hit);
      HighwayUtils.swapOffhand();
      if (mc.player.getMainHandStack().getItem() instanceof BlockItem bi) {
         mc.world.setBlockState(pos, bi.getBlock().getDefaultState(), 3);
      }
   }

   @EventHandler
   private void onRender3D(Render3DEvent event) {
      if (mc.player != null && mc.world != null && (Boolean)this.render2.get() && this.placePos != null) {
         if (mc.player.getMainHandStack().getItem() instanceof BlockItem
            || mc.player.getMainHandStack().getItem() instanceof SpawnEggItem) {
            if (mc.world.getBlockState(this.placePos).isReplaceable()) {
               BlockRenderUtils.renderPosition(event, this.placePos, Block.getBlockFromItem(mc.player.getMainHandStack().getItem()));
            }
         }
      }
   }
}
